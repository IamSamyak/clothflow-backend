package com.clothflow.order.service;

import com.clothflow.order.dto.response.OrderItemResponse;
import com.clothflow.order.dto.response.OrderResponse;
import com.clothflow.order.entity.InventoryReservationStatus;
import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderInventoryReservation;
import com.clothflow.order.entity.OrderItem;
import com.clothflow.order.entity.OrderStatus;
import com.clothflow.order.entity.OrderStatusHistory;
import com.clothflow.order.exception.InvalidOrderStatusTransitionException;
import com.clothflow.order.exception.OrderNotFoundException;
import com.clothflow.order.outbox.OrderOutboxService;
import com.clothflow.order.repository.OrderInventoryReservationRepository;
import com.clothflow.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderStatePersistenceService {

    private final OrderRepository orderRepository;
    private final OrderInventoryReservationRepository reservationRepository;
    private final OrderStatusTransitionService statusTransitionService;
    private final OrderOutboxService orderOutboxService;

    public OrderStatePersistenceService(
            OrderRepository orderRepository,
            OrderInventoryReservationRepository reservationRepository,
            OrderStatusTransitionService statusTransitionService, OrderOutboxService orderOutboxService
    ) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.statusTransitionService = statusTransitionService;
        this.orderOutboxService = orderOutboxService;
    }

    @Transactional
    public void markReservationReserved(UUID reservationId) {

        OrderInventoryReservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order inventory reservation not found: "
                                                + reservationId
                                )
                        );

        reservation.setStatus(
                InventoryReservationStatus.RESERVED
        );
    }

    @Transactional
    public void markReservationReleased(UUID reservationId) {

        OrderInventoryReservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order inventory reservation not found: "
                                                + reservationId
                                )
                        );

        reservation.setStatus(
                InventoryReservationStatus.RELEASED
        );
    }

    @Transactional
    public void markReservationReleaseFailed(
            UUID reservationId
    ) {

        OrderInventoryReservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order inventory reservation not found: "
                                                + reservationId
                                )
                        );

        reservation.setStatus(
                InventoryReservationStatus.RELEASE_FAILED
        );
    }

    @Transactional
    public void markReservationCommitted(
            UUID reservationId
    ) {

        OrderInventoryReservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order inventory reservation not found: "
                                                + reservationId
                                )
                        );

        if (reservation.getStatus()
                != InventoryReservationStatus.RESERVED) {

            throw new IllegalStateException(
                    "Cannot commit inventory reservation "
                            + reservationId
                            + " because current status is "
                            + reservation.getStatus()
            );
        }

        reservation.setStatus(
                InventoryReservationStatus.COMMITTED
        );
    }

    @Transactional
    public void markInventoryReserved(
            UUID orderId
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        statusTransitionService.validateTransition(
                order.getStatus(),
                OrderStatus.INVENTORY_RESERVED
        );

        OrderStatus currentStatus =
                order.getStatus();

        order.setStatus(
                OrderStatus.INVENTORY_RESERVED
        );

        order.addStatusHistory(
                new OrderStatusHistory(
                        currentStatus,
                        OrderStatus.INVENTORY_RESERVED,
                        "All inventory reservations succeeded"
                )
        );
    }

    @Transactional
    public void markInventoryFailed(
            UUID orderId,
            String reason
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        statusTransitionService.validateTransition(
                order.getStatus(),
                OrderStatus.INVENTORY_FAILED
        );

        OrderStatus currentStatus =
                order.getStatus();

        order.setStatus(
                OrderStatus.INVENTORY_FAILED
        );

        order.addStatusHistory(
                new OrderStatusHistory(
                        currentStatus,
                        OrderStatus.INVENTORY_FAILED,
                        reason
                )
        );
    }

    @Transactional
    public void cancelOrderWithoutInventory(
            UUID orderId,
            String reason
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        OrderStatus currentStatus =
                order.getStatus();

        statusTransitionService.validateTransition(
                currentStatus,
                OrderStatus.CANCELLED
        );

        order.setStatus(
                OrderStatus.CANCELLED
        );

        order.addStatusHistory(
                new OrderStatusHistory(
                        currentStatus,
                        OrderStatus.CANCELLED,
                        reason != null
                                ? reason
                                : "Order cancelled"
                )
        );
    }

    @Transactional
    public OrderResponse updateStatus(
            UUID orderId,
            OrderStatus newStatus,
            String reason
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        OrderStatus currentStatus =
                order.getStatus();

        statusTransitionService.validateTransition(
                currentStatus,
                newStatus
        );

        order.setStatus(newStatus);

        order.addStatusHistory(
                new OrderStatusHistory(
                        currentStatus,
                        newStatus,
                        reason != null
                                ? reason
                                : "Order status updated"
                )
        );

        return toResponse(order);
    }

    @Transactional
    public OrderResponse markOrderConfirmed(
            UUID orderId
    ) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new OrderNotFoundException(
                                        orderId
                                )
                        );

        OrderStatus currentStatus =
                order.getStatus();

        statusTransitionService.validateTransition(
                currentStatus,
                OrderStatus.CONFIRMED
        );

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        order.addStatusHistory(
                new OrderStatusHistory(
                        currentStatus,
                        OrderStatus.CONFIRMED,
                        "Payment successful and inventory committed"
                )
        );

        Order savedOrder =
                orderRepository.saveAndFlush(order);

        /*
         * Create ORDER_CONFIRMED outbox event in the
         * same database transaction as the order update.
         *
         * The event will NOT be published to Kafka here.
         * The outbox publisher will publish it asynchronously.
         */
        orderOutboxService.createOrderConfirmedEvent(
                savedOrder
        );

        return toResponse(savedOrder);
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items =
                order.getItems()
                        .stream()
                        .map(this::toItemResponse)
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

    private OrderItemResponse toItemResponse(
            OrderItem item
    ) {

        return new OrderItemResponse(
                item.getProductId(),
                item.getSku(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    @Transactional
    public PaymentTransitionResult markPaymentPending(
            UUID orderId
    ) {

        int updatedRows =
                orderRepository.transitionStatusAtomically(
                        orderId,
                        OrderStatus.INVENTORY_RESERVED,
                        OrderStatus.PAYMENT_PENDING
                );

        /*
         * This request successfully won the race.
         *
         * INVENTORY_RESERVED
         *        ↓
         * PAYMENT_PENDING
         */
        if (updatedRows == 1) {

            Order order =
                    orderRepository.findById(orderId)
                            .orElseThrow(
                                    () -> new OrderNotFoundException(
                                            orderId
                                    )
                            );

            order.addStatusHistory(
                    new OrderStatusHistory(
                            OrderStatus.INVENTORY_RESERVED,
                            OrderStatus.PAYMENT_PENDING,
                            "Payment processing started"
                    )
            );

            return new PaymentTransitionResult(
                    toResponse(order),
                    true
            );
        }

        /*
         * Another concurrent request already changed the order.
         *
         * Reload the latest committed state.
         */
        Order currentOrder =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        orderId
                                )
                        );

        /*
         * The normal concurrent case:
         *
         * Request A:
         * INVENTORY_RESERVED -> PAYMENT_PENDING
         *
         * Request B:
         * atomic update affects 0 rows
         * because status is already PAYMENT_PENDING.
         */
        if (currentOrder.getStatus()
                == OrderStatus.PAYMENT_PENDING) {

            return new PaymentTransitionResult(
                    toResponse(currentOrder),
                    false
            );
        }

        /*
         * If the order moved somewhere else, don't silently
         * accept the situation.
         */
        throw new InvalidOrderStatusTransitionException(
                currentOrder.getStatus(),
                OrderStatus.PAYMENT_PENDING
        );
    }

    @Transactional
    public void confirmPayment(
            Order order,
            String paymentReference
    ) {

        OrderStatus currentStatus =
                order.getStatus();

        /*
         * Idempotency:
         *
         * If the order is already CONFIRMED, there is nothing
         * more to do.
         */
        if (currentStatus == OrderStatus.CONFIRMED) {
            return;
        }

        /*
         * Payment can only confirm an order that is currently
         * waiting for payment.
         */
        statusTransitionService.validateTransition(
                currentStatus,
                OrderStatus.CONFIRMED
        );

        /*
         * Move:
         *
         * PAYMENT_PENDING
         *        ↓
         * CONFIRMED
         */
        order.setStatus(
                OrderStatus.CONFIRMED
        );

        /*
         * Record the business state transition.
         */
        order.addStatusHistory(
                new OrderStatusHistory(
                        currentStatus,
                        OrderStatus.CONFIRMED,
                        "Payment succeeded: " + paymentReference
                )
        );

        /*
         * Persist the confirmed order.
         *
         * This operation and the outbox insert below are part
         * of the SAME database transaction.
         */
        Order savedOrder =
                orderRepository.saveAndFlush(order);

        /*
         * Create ORDER_CONFIRMED outbox event.
         *
         * The event is NOT sent to Kafka here.
         *
         * It is stored in the same database transaction and will
         * later be published by the OutboxPublisherService.
         */
        orderOutboxService.createOrderConfirmedEvent(
                savedOrder
        );
    }

    @Transactional
    public OrderResponse markShipped(UUID orderId) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(orderId)
                        );

        if (order.getStatus() == OrderStatus.SHIPPED) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusTransitionException(
                    order.getStatus(),
                    OrderStatus.SHIPPED
            );
        }

        OrderStatus previousStatus =
                order.getStatus();

        order.setStatus(
                OrderStatus.SHIPPED
        );

        order.addStatusHistory(
                new OrderStatusHistory(
                        previousStatus,
                        OrderStatus.SHIPPED,
                        "Shipment shipped"
                )
        );

        return toResponse(order);
    }

    @Transactional
    public OrderResponse markDelivered(UUID orderId) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(orderId)
                        );

        if (order.getStatus() == OrderStatus.DELIVERED) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderStatusTransitionException(
                    order.getStatus(),
                    OrderStatus.DELIVERED
            );
        }

        OrderStatus previousStatus =
                order.getStatus();

        order.setStatus(
                OrderStatus.DELIVERED
        );

        order.addStatusHistory(
                new OrderStatusHistory(
                        previousStatus,
                        OrderStatus.DELIVERED,
                        "Shipment delivered"
                )
        );

        return toResponse(order);
    }

    @Transactional
    public void markReservationRestored(UUID reservationId) {

        OrderInventoryReservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order inventory reservation not found: "
                                                + reservationId
                                )
                        );

        if (reservation.getStatus()
                != InventoryReservationStatus.COMMITTED) {

            throw new IllegalStateException(
                    "Cannot mark inventory reservation "
                            + reservationId
                            + " as RESTORED because current status is "
                            + reservation.getStatus()
            );
        }

        reservation.setStatus(
                InventoryReservationStatus.RESTORED
        );
    }
}