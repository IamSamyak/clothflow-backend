package com.clothflow.order.service;

import com.clothflow.order.client.InventoryClient;
import com.clothflow.order.client.PaymentClient;
import com.clothflow.order.client.ProductClient;
import com.clothflow.order.dto.request.CreateOrderRequest;
import com.clothflow.order.dto.response.OrderItemResponse;
import com.clothflow.order.dto.response.OrderResponse;
import com.clothflow.order.dto.response.PaymentResponse;
import com.clothflow.order.entity.InventoryReservationStatus;
import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderInventoryReservation;
import com.clothflow.order.entity.OrderStatus;
import com.clothflow.order.entity.PaymentStatus;
import com.clothflow.order.exception.InventoryInsufficientStockException;
import com.clothflow.order.exception.InventoryServiceException;
import com.clothflow.order.exception.OrderNotFoundException;
import com.clothflow.order.exception.PaymentServiceException;
import com.clothflow.order.metrics.OrderMetrics;
import com.clothflow.order.repository.OrderInventoryReservationRepository;
import com.clothflow.order.repository.OrderRepository;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderInventoryReservationRepository reservationRepository;
    private final InventoryClient inventoryClient;
    private final OrderCreationPersistenceService orderCreationPersistenceService;
    private final OrderStatePersistenceService orderStatePersistenceService;
    private final OrderReadService orderReadService;
    private final PaymentClient paymentClient;
    private final OrderMetrics orderMetrics;

    public OrderService(
            OrderRepository orderRepository,
            ProductClient productClient,
            OrderStatusTransitionService statusTransitionService,
            OrderInventoryReservationRepository reservationRepository,
            InventoryClient inventoryClient,
            OrderCreationPersistenceService orderCreationPersistenceService,
            OrderStatePersistenceService orderStatePersistenceService,
            OrderReadService orderReadService,
            PaymentClient paymentClient,
            OrderMetrics orderMetrics
    ) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryClient = inventoryClient;
        this.orderCreationPersistenceService =
                orderCreationPersistenceService;
        this.orderStatePersistenceService =
                orderStatePersistenceService;
        this.orderReadService = orderReadService;
        this.paymentClient = paymentClient;
        this.orderMetrics = orderMetrics;
    }

    /**
     * Creates an order and synchronously reserves inventory.
     *
     * Flow:
     *
     * 1. Create PENDING order in Order DB.
     * 2. Create inventory reservation records with stable reservation IDs.
     * 3. Reserve inventory for each order item.
     * 4. If all reservations succeed:
     *      PENDING -> INVENTORY_RESERVED
     *
     * 5. If inventory is insufficient:
     *      release previously successful reservations
     *      PENDING -> INVENTORY_FAILED
     *
     * 6. If Inventory Service is unavailable:
     *      leave order PENDING
     *      propagate service-unavailable error.
     *
     * Important:
     * Order DB and Inventory DB are completely independent
     * transaction boundaries.
     *
     * Metrics:
     *
     * - order.created is emitted after the local order transaction commits.
     * - inventory.reserved is emitted after each remote reservation succeeds.
     * - inventory.failed is emitted only for definitive insufficient stock.
     * - processing timer covers the synchronous order + inventory workflow.
     */
    public OrderResponse createOrder(
            CreateOrderRequest request,
            UUID customerId
    ) {

        Timer.Sample processingTimer =
                orderMetrics.startProcessingTimer();

        Order order =
                orderCreationPersistenceService
                        .createPendingOrder(
                                request,
                                customerId
                        );

        /*
         * The local order transaction has successfully committed.
         *
         * This is therefore a genuine business event.
         */
        orderMetrics.orderCreated();

        log.info(
                "Order created in PENDING state: orderId={}, customerId={}",
                order.getId(),
                order.getCustomerId()
        );

        /*
         * The transaction inside createPendingOrder()
         * has already committed.
         *
         * We now intentionally cross the distributed
         * transaction boundary and communicate with
         * Inventory Service.
         */
        List<OrderInventoryReservation> reservations =
                reservationRepository.findAllByOrderId(
                        order.getId()
                );

        log.info(
                "Starting inventory reservation: orderId={}, reservationCount={}",
                order.getId(),
                reservations.size()
        );

        try {

            /*
             * Reserve inventory one item at a time.
             *
             * Each reservation has a stable reservationId.
             *
             * Inventory Service uses reservationId for
             * idempotency.
             */
            for (OrderInventoryReservation reservation :
                    reservations) {

                log.info(
                        "Reserving inventory: orderId={}, reservationId={}, " +
                                "productId={}, quantity={}",
                        order.getId(),
                        reservation.getReservationId(),
                        reservation.getProductId(),
                        reservation.getQuantity()
                );

                inventoryClient.reserve(
                        reservation.getProductId(),
                        reservation.getReservationId(),
                        reservation.getQuantity()
                );

                /*
                 * Persist the local state only after
                 * Inventory confirms success.
                 */
                orderStatePersistenceService
                        .markReservationReserved(
                                reservation.getId()
                        );

                /*
                 * This is a genuine successful inventory
                 * reservation event.
                 */
                orderMetrics.inventoryReserved();

                log.info(
                        "Inventory reservation successful: orderId={}, reservationId={}",
                        order.getId(),
                        reservation.getReservationId()
                );
            }

            /*
             * Every inventory reservation succeeded.
             *
             * PENDING -> INVENTORY_RESERVED
             */
            orderStatePersistenceService
                    .markInventoryReserved(
                            order.getId()
                    );

            log.info(
                    "Order inventory reserved successfully: orderId={}",
                    order.getId()
            );

            OrderResponse response =
                    getOrder(order.getId());

            /*
             * The complete synchronous order creation
             * workflow has now completed successfully.
             */
            orderMetrics.recordProcessing(processingTimer);

            return response;

        } catch (InventoryInsufficientStockException ex) {

            /*
             * This is a definitive business failure.
             */
            orderMetrics.inventoryFailed();

            log.warn(
                    "Insufficient inventory for order: orderId={}, productId={}",
                    order.getId(),
                    ex.getProductId()
            );

            /*
             * One inventory reservation failed.
             *
             * Release every reservation that succeeded
             * before the failure.
             */
            compensateReservations(
                    order.getId()
            );

            /*
             * Mark the Order as failed after compensation
             * processing.
             */
            orderStatePersistenceService
                    .markInventoryFailed(
                            order.getId(),
                            "Insufficient inventory for product "
                                    + ex.getProductId()
                    );

            log.warn(
                    "Order marked INVENTORY_FAILED: orderId={}",
                    order.getId()
            );

            OrderResponse response =
                    getOrder(order.getId());

            orderMetrics.recordProcessing(processingTimer);

            return response;

        } catch (InventoryServiceException ex) {

            /*
             * IMPORTANT:
             *
             * Do not emit inventory.failed here.
             *
             * The remote Inventory operation may actually have
             * succeeded and only the response was lost.
             */
            log.error(
                    "Inventory Service communication failed: orderId={}",
                    order.getId(),
                    ex
            );

            /*
             * Do NOT automatically mark the order
             * INVENTORY_FAILED.
             *
             * Inventory may have processed the reservation
             * even though the response timed out.
             *
             * reservationId provides idempotency for retry.
             */
            throw ex;
        }
    }

    /**
     * Compensates all reservations that were successfully
     * created before another reservation failed.
     */
    private void compensateReservations(
            UUID orderId
    ) {

        List<OrderInventoryReservation> reservations =
                reservationRepository.findAllByOrderId(
                        orderId
                );

        log.info(
                "Starting inventory compensation: orderId={}, reservationCount={}",
                orderId,
                reservations.size()
        );

        for (OrderInventoryReservation reservation :
                reservations) {

            /*
             * Only reservations confirmed as RESERVED
             * require compensation.
             */
            if (reservation.getStatus()
                    != InventoryReservationStatus.RESERVED) {

                continue;
            }

            try {

                log.info(
                        "Releasing inventory during compensation: " +
                                "orderId={}, reservationId={}, productId={}, quantity={}",
                        orderId,
                        reservation.getReservationId(),
                        reservation.getProductId(),
                        reservation.getQuantity()
                );

                inventoryClient.release(
                        reservation.getProductId(),
                        reservation.getReservationId(),
                        reservation.getQuantity()
                );

                orderStatePersistenceService
                        .markReservationReleased(
                                reservation.getId()
                        );

                log.info(
                        "Inventory compensation successful: orderId={}, reservationId={}",
                        orderId,
                        reservation.getReservationId()
                );

            } catch (InventoryServiceException ex) {

                log.error(
                        "Inventory compensation failed: " +
                                "orderId={}, reservationId={}, productId={}",
                        orderId,
                        reservation.getReservationId(),
                        reservation.getProductId(),
                        ex
                );

                /*
                 * Inventory may still hold the reservation.
                 *
                 * Preserve that fact locally.
                 */
                orderStatePersistenceService
                        .markReservationReleaseFailed(
                                reservation.getId()
                        );
            }
        }
    }

    /**
     * Retrieves an order by ID.
     */
    public OrderResponse getOrder(UUID orderId) {

        return orderReadService.getOrder(orderId);
    }

    /**
     * Retrieves an order belonging to a specific customer.
     */
    public OrderResponse getOrder(
            UUID orderId,
            UUID customerId
    ) {

        return orderReadService.getOrder(
                orderId,
                customerId
        );
    }

    /**
     * Updates an order status after validating
     * the allowed state transition.
     */
    public OrderResponse updateStatus(
            UUID orderId,
            OrderStatus newStatus,
            String reason
    ) {

        return orderStatePersistenceService
                .updateStatus(
                        orderId,
                        newStatus,
                        reason
                );
    }

    /**
     * Commits all inventory reservations after successful
     * payment.
     *
     * Flow:
     *
     * PAYMENT_PENDING
     *       ↓
     * Inventory commit
     *       ↓
     * Reservations -> COMMITTED
     *       ↓
     * CONFIRMED
     */
    public OrderResponse commitInventory(
            UUID orderId
    ) {

        Order order =
                orderRepository.findById(
                                orderId
                        )
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        orderId
                                )
                        );

        if (order.getStatus()
                != OrderStatus.PAYMENT_PENDING) {

            throw new IllegalStateException(
                    "Inventory can only be committed when order is PAYMENT_PENDING"
            );
        }

        List<OrderInventoryReservation> reservations =
                reservationRepository.findAllByOrderId(
                        orderId
                );

        log.info(
                "Starting inventory commit: orderId={}, reservationCount={}",
                orderId,
                reservations.size()
        );

        for (OrderInventoryReservation reservation :
                reservations) {

            if (reservation.getStatus()
                    != InventoryReservationStatus.RESERVED) {

                throw new IllegalStateException(
                        "Reservation "
                                + reservation.getReservationId()
                                + " is not in RESERVED state"
                );
            }

            log.info(
                    "Committing inventory reservation: " +
                            "orderId={}, reservationId={}, productId={}, quantity={}",
                    orderId,
                    reservation.getReservationId(),
                    reservation.getProductId(),
                    reservation.getQuantity()
            );

            inventoryClient.commit(
                    reservation.getProductId(),
                    reservation.getReservationId(),
                    reservation.getQuantity()
            );

            orderStatePersistenceService
                    .markReservationCommitted(
                            reservation.getId()
                    );

            log.info(
                    "Inventory reservation committed: orderId={}, reservationId={}",
                    orderId,
                    reservation.getReservationId()
            );
        }

        OrderResponse response =
                orderStatePersistenceService
                        .markOrderConfirmed(
                                orderId
                        );

        /*
         * CONFIRMED is now actually persisted.
         */
        orderMetrics.orderConfirmed();

        log.info(
                "Order confirmed after inventory commit: orderId={}",
                orderId
        );

        return response;
    }

    /**
     * Cancels an order before payment completion.
     *
     * Cancellation is idempotent:
     *
     * - CANCELLED -> return the existing order
     * - PENDING -> cancel without inventory
     * - INVENTORY_RESERVED -> release inventory, then cancel
     * - PAYMENT_PENDING -> release inventory, then cancel
     *
     * Confirmed orders are intentionally not cancelled through
     * this endpoint.
     *
     * Post-payment cancellation follows a separate flow:
     *
     * CONFIRMED
     *     ↓
     * Payment refund
     *     ↓
     * PAYMENT_REFUNDED event
     *     ↓
     * Restore committed inventory
     *     ↓
     * CANCELLED
     */
    public OrderResponse cancelOrder(
            UUID orderId,
            UUID customerId,
            String reason
    ) {

        Order order =
                orderRepository
                        .findByIdAndCustomerId(
                                orderId,
                                customerId
                        )
                        .orElseThrow(
                                () -> new OrderNotFoundException(orderId)
                        );

        OrderStatus currentStatus =
                order.getStatus();

        log.info(
                "Cancelling order: orderId={}, currentStatus={}, reason={}",
                orderId,
                currentStatus,
                reason
        );

        /*
         * Idempotent cancellation.
         *
         * If the order was already cancelled, this request
         * has already achieved its intended result.
         *
         * IMPORTANT:
         *
         * We do NOT call releaseOrderReservations().
         *
         * The first cancellation already released the inventory
         * and changed the local reservation state.
         */
        if (currentStatus == OrderStatus.CANCELLED) {

            log.info(
                    "Order cancellation already completed: " +
                            "orderId={}, status=CANCELLED",
                    orderId
            );

            return getOrder(orderId);
        }

        /*
         * No inventory reservation exists.
         *
         * PENDING -> CANCELLED
         */
        if (currentStatus == OrderStatus.PENDING) {

            log.info(
                    "Cancelling PENDING order without inventory release: " +
                            "orderId={}",
                    orderId
            );

            orderStatePersistenceService
                    .cancelOrderWithoutInventory(
                            orderId,
                            reason
                    );

            orderMetrics.orderCancelled();

            log.info(
                    "Order cancelled successfully: " +
                            "orderId={}, previousStatus=PENDING",
                    orderId
            );

            return getOrder(orderId);
        }

        /*
         * Inventory has been reserved but payment has not
         * successfully completed.
         *
         * Release inventory before cancelling the order.
         *
         * INVENTORY_RESERVED -> release -> CANCELLED
         *
         * PAYMENT_PENDING -> release -> CANCELLED
         */
        if (currentStatus == OrderStatus.INVENTORY_RESERVED
                || currentStatus == OrderStatus.PAYMENT_PENDING) {

            log.info(
                    "Releasing inventory reservations before cancellation: " +
                            "orderId={}, currentStatus={}",
                    orderId,
                    currentStatus
            );

            releaseOrderReservations(orderId);

            orderStatePersistenceService
                    .cancelOrderWithoutInventory(
                            orderId,
                            reason
                    );

            orderMetrics.orderCancelled();

            log.info(
                    "Order cancelled successfully: " +
                            "orderId={}, previousStatus={}",
                    orderId,
                    currentStatus
            );

            return getOrder(orderId);
        }

        /*
         * Confirmed/post-payment cancellation is not yet
         * supported through this endpoint because refund
         * processing follows a separate event-driven flow.
         */
        log.warn(
                "Order cancellation rejected: " +
                        "orderId={}, currentStatus={}",
                orderId,
                currentStatus
        );

        throw new IllegalStateException(
                "Order cannot be cancelled from status "
                        + currentStatus
        );
    }

    /**
     * Completes post-payment cancellation after a successful refund.
     *
     * CONFIRMED
     *     ↓
     * Payment refunded
     *     ↓
     * Restore stock
     *     ↓
     * CANCELLED
     */
    @Transactional
    public OrderResponse completeRefundedCancellation(
            UUID orderId,
            String reason
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(orderId)
                        );

        OrderStatus currentStatus =
                order.getStatus();

        /*
         * Idempotency:
         *
         * If the order was already cancelled, the refund event
         * has already completed its business effect.
         */
        if (currentStatus == OrderStatus.CANCELLED) {
            return getOrder(orderId);
        }

        if (currentStatus != OrderStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Cannot complete refunded cancellation for order "
                            + orderId
                            + " because current status is "
                            + currentStatus
            );
        }

        List<OrderInventoryReservation> reservations =
                reservationRepository.findAllByOrderId(orderId);

        for (OrderInventoryReservation reservation :
                reservations) {

            /*
             * Payment success causes the inventory reservation
             * to become COMMITTED.
             *
             * A COMMITTED reservation cannot be released.
             *
             * The stock has already been consumed, so after a
             * successful refund we restore the stock instead.
             */
            if (reservation.getStatus()
                    == InventoryReservationStatus.COMMITTED) {

                UUID restorationOperationId =
                        UUID.nameUUIDFromBytes(
                                (
                                        "clothflow:stock-restoration:"
                                                + orderId
                                                + ":"
                                                + reservation.getId()
                                ).getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

                inventoryClient.addStock(
                        reservation.getProductId(),
                        restorationOperationId,
                        reservation.getQuantity()
                );

                orderStatePersistenceService
                        .markReservationRestored(
                                reservation.getId()
                        );
            }

            /*
             * If this reservation was already restored,
             * don't perform the Inventory operation again.
             */
            else if (
                    reservation.getStatus()
                            == InventoryReservationStatus.RESTORED
            ) {

                // Already restored. Nothing to do.
            }
        }

        orderStatePersistenceService.cancelOrderWithoutInventory(
                orderId,
                reason != null
                        ? reason
                        : "Payment refunded: order cancelled"
        );

        /*
         * The refunded cancellation is now locally complete.
         */
        orderMetrics.orderCancelled();

        return getOrder(orderId);
    }

    private void releaseOrderReservations(
            UUID orderId
    ) {

        List<OrderInventoryReservation> reservations =
                reservationRepository.findAllByOrderId(
                        orderId
                );

        log.info(
                "Found inventory reservations for cancellation: " +
                        "orderId={}, reservationCount={}",
                orderId,
                reservations.size()
        );

        for (OrderInventoryReservation reservation :
                reservations) {

            /*
             * Only currently RESERVED inventory should
             * be released.
             */
            if (reservation.getStatus()
                    != InventoryReservationStatus.RESERVED) {

                log.debug(
                        "Skipping inventory reservation: " +
                                "orderId={}, reservationId={}, status={}",
                        orderId,
                        reservation.getReservationId(),
                        reservation.getStatus()
                );

                continue;
            }

            try {

                log.info(
                        "Releasing inventory reservation: " +
                                "orderId={}, reservationId={}, productId={}, quantity={}",
                        orderId,
                        reservation.getReservationId(),
                        reservation.getProductId(),
                        reservation.getQuantity()
                );

                inventoryClient.release(
                        reservation.getProductId(),
                        reservation.getReservationId(),
                        reservation.getQuantity()
                );

                orderStatePersistenceService
                        .markReservationReleased(
                                reservation.getId()
                        );

                log.info(
                        "Inventory reservation released successfully: " +
                                "orderId={}, reservationId={}",
                        orderId,
                        reservation.getReservationId()
                );

            } catch (InventoryServiceException ex) {

                log.error(
                        "Failed to release inventory reservation: " +
                                "orderId={}, reservationId={}, productId={}, quantity={}",
                        orderId,
                        reservation.getReservationId(),
                        reservation.getProductId(),
                        reservation.getQuantity(),
                        ex
                );

                /*
                 * Inventory may still be reserved.
                 */
                orderStatePersistenceService
                        .markReservationReleaseFailed(
                                reservation.getId()
                        );

                throw ex;
            }
        }
    }

    /**
     * Maps the domain entity to the API response.
     */
    private OrderResponse toResponse(
            Order order
    ) {

        List<OrderItemResponse> items =
                order.getItems()
                        .stream()
                        .map(
                                item ->
                                        new OrderItemResponse(
                                                item.getProductId(),
                                                item.getSku(),
                                                item.getProductName(),
                                                item.getUnitPrice(),
                                                item.getQuantity(),
                                                item.getSubtotal()
                                        )
                        )
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * Moves the order:
     *
     * INVENTORY_RESERVED -> PAYMENT_PENDING
     *
     * Returns whether this request actually performed
     * the transition.
     */
    public PaymentTransitionResult markPaymentPending(
            UUID orderId
    ) {

        return orderStatePersistenceService
                .markPaymentPending(
                        orderId
                );
    }

    /**
     * Starts payment processing for an order.
     *
     * Flow:
     *
     * INVENTORY_RESERVED
     *        ↓
     * PAYMENT_PENDING
     *        ↓
     * Payment Service
     *        ↓
     * ┌───────────────┬───────────────┐
     * │               │               │
     * SUCCEEDED       FAILED          other
     * │               │               │
     * ↓               ↓               ↓
     * Commit          Release         Error
     * Inventory       Inventory
     * │               │
     * ↓               ↓
     * CONFIRMED       PAYMENT_FAILED
     *
     * Metrics:
     *
     * - payment.succeeded only for definitive SUCCEEDED response.
     * - payment.failed only for definitive FAILED response.
     * - ambiguous communication errors emit neither.
     */
    public OrderResponse processPayment(
            UUID orderId,
            UUID customerId
    ) {

        Timer.Sample paymentTimer =
                orderMetrics.startProcessingTimer();

        Order order =
                orderRepository
                        .findByIdAndCustomerId(
                                orderId,
                                customerId
                        )
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                orderId
                                        )
                        );

        log.info(
                "Starting payment processing: orderId={}, status={}",
                orderId,
                order.getStatus()
        );

        if (order.getStatus() == OrderStatus.CONFIRMED) {

            log.info(
                    "Payment already completed for order: orderId={}, status=CONFIRMED",
                    orderId
            );

            return getOrder(orderId);
        }

        if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {

            log.info(
                    "Payment already failed for order: orderId={}, status=PAYMENT_FAILED",
                    orderId
            );

            return getOrder(orderId);
        }

        if (order.getStatus() != OrderStatus.INVENTORY_RESERVED) {

            log.warn(
                    "Payment rejected because order is not INVENTORY_RESERVED: " +
                            "orderId={}, currentStatus={}",
                    orderId,
                    order.getStatus()
            );

            throw new IllegalStateException(
                    "Payment can only be started when order "
                            + "is INVENTORY_RESERVED"
            );
        }

        PaymentTransitionResult transitionResult =
                orderStatePersistenceService
                        .markPaymentPending(
                                orderId
                        );

        if (!transitionResult.transitioned()) {

            /*
             * Another concurrent request already won the
             * INVENTORY_RESERVED -> PAYMENT_PENDING transition.
             *
             * This request must NOT call Payment Service.
             *
             * Returning the current state makes the /pay endpoint
             * safe for concurrent duplicate callers.
             */
            log.info(
                    "Payment already being processed by another request: " +
                            "orderId={}, status={}",
                    orderId,
                    transitionResult.order().status()
            );

            return transitionResult.order();
        }

        /*
         * Stable idempotency key.
         *
         * If this operation is retried, Payment Service
         * receives the same key and returns the existing
         * payment instead of creating another payment.
         */
        String idempotencyKey =
                "order-payment-" + orderId;

        log.info(
                "Calling Payment Service: orderId={}, idempotencyKey={}",
                orderId,
                idempotencyKey
        );

        try {

            PaymentResponse payment =
                    paymentClient.createPayment(
                            order,
                            idempotencyKey
                    );

            log.info(
                    "Payment Service response received: " +
                            "orderId={}, paymentId={}, paymentStatus={}",
                    orderId,
                    payment.id(),
                    payment.status()
            );

            /*
             * Payment Service owns its own PaymentStatus enum.
             *
             * Order Service owns a separate PaymentStatus enum.
             *
             * The two services communicate through JSON.
             */
            if (payment.status()
                    == PaymentStatus.SUCCEEDED) {

                /*
                 * Definitive payment success.
                 */
                orderMetrics.paymentSucceeded();

                log.info(
                        "Payment succeeded: orderId={}, paymentId={}",
                        orderId,
                        payment.id()
                );

                OrderResponse response =
                        commitInventoryAfterPayment(
                                orderId
                        );

                orderMetrics.recordProcessing(paymentTimer);

                return response;
            }

            if (payment.status()
                    == PaymentStatus.FAILED) {

                /*
                 * Definitive payment failure.
                 */
                orderMetrics.paymentFailed();

                log.warn(
                        "Payment failed: orderId={}, paymentId={}",
                        orderId,
                        payment.id()
                );

                handlePaymentFailure(
                        orderId,
                        "Payment failed"
                );

                OrderResponse response =
                        getOrder(orderId);

                orderMetrics.recordProcessing(paymentTimer);

                return response;
            }

            /*
             * An unexpected status is not treated as a
             * definitive payment failure.
             */
            throw new PaymentServiceException(
                    "Unexpected payment status: "
                            + payment.status()
            );

        } catch (PaymentServiceException ex) {

            /*
             * IMPORTANT:
             *
             * Do NOT emit payment.failed here.
             *
             * Payment Service may have successfully processed
             * the payment while the response was lost.
             *
             * The stable idempotency key allows safe retry.
             */
            log.error(
                    "Payment Service communication failed: " +
                            "orderId={}, idempotencyKey={}",
                    orderId,
                    idempotencyKey,
                    ex
            );

            throw ex;
        }
    }

    /**
     * Handles a definitive payment failure.
     *
     * Payment has definitely failed, so the inventory
     * reservation must be compensated.
     */
    private void handlePaymentFailure(
            UUID orderId,
            String reason
    ) {

        log.info(
                "Starting payment failure compensation: " +
                        "orderId={}, reason={}",
                orderId,
                reason
        );

        releaseOrderReservations(
                orderId
        );

        orderStatePersistenceService
                .updateStatus(
                        orderId,
                        OrderStatus.PAYMENT_FAILED,
                        reason
                );

        log.warn(
                "Order marked PAYMENT_FAILED after compensation: orderId={}",
                orderId
        );
    }

    /**
     * Commits inventory after successful payment.
     *
     * Payment:
     *
     * PENDING -> SUCCEEDED
     *
     * Then:
     *
     * Inventory:
     * RESERVED -> COMMITTED
     *
     * Finally:
     *
     * Order:
     * PAYMENT_PENDING -> CONFIRMED
     */
    private OrderResponse commitInventoryAfterPayment(
            UUID orderId
    ) {

        List<OrderInventoryReservation> reservations =
                reservationRepository.findAllByOrderId(
                        orderId
                );

        log.info(
                "Starting inventory commit after payment: " +
                        "orderId={}, reservationCount={}",
                orderId,
                reservations.size()
        );

        for (
                OrderInventoryReservation reservation :
                reservations
        ) {

            if (reservation.getStatus()
                    != InventoryReservationStatus.RESERVED) {

                throw new IllegalStateException(
                        "Reservation "
                                + reservation.getReservationId()
                                + " is not RESERVED"
                );
            }

            log.info(
                    "Committing inventory after payment: " +
                            "orderId={}, reservationId={}, productId={}, quantity={}",
                    orderId,
                    reservation.getReservationId(),
                    reservation.getProductId(),
                    reservation.getQuantity()
            );

            /*
             * Remote Inventory operation.
             *
             * reservationId makes the operation idempotent.
             */
            inventoryClient.commit(
                    reservation.getProductId(),
                    reservation.getReservationId(),
                    reservation.getQuantity()
            );

            /*
             * Persist local state only after Inventory
             * confirms successful commit.
             */
            orderStatePersistenceService
                    .markReservationCommitted(
                            reservation.getId()
                    );

            log.info(
                    "Inventory reservation committed after payment: " +
                            "orderId={}, reservationId={}",
                    orderId,
                    reservation.getReservationId()
            );
        }

        /*
         * All inventory reservations committed.
         *
         * PAYMENT_PENDING -> CONFIRMED
         */
        OrderResponse response =
                orderStatePersistenceService
                        .markOrderConfirmed(
                                orderId
                        );

        /*
         * The order is now definitively CONFIRMED.
         */
        orderMetrics.orderConfirmed();

        log.info(
                "Order confirmed successfully: orderId={}",
                orderId
        );

        return response;
    }
}