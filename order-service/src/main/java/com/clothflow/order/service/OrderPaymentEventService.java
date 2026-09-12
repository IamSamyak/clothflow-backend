package com.clothflow.order.service;

import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderStatus;
import com.clothflow.order.event.PaymentRefundedEvent;
import com.clothflow.order.event.PaymentSucceededEvent;
import com.clothflow.order.exception.OrderNotFoundException;
import com.clothflow.order.outbox.OrderOutboxService;
import com.clothflow.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderPaymentEventService {

    private final OrderRepository orderRepository;

    private final OrderStatePersistenceService
            orderStatePersistenceService;

    private final ProcessedEventService
            processedEventService;

    private final OrderService orderService;

    private final OrderOutboxService orderOutboxService;

    public OrderPaymentEventService(
            OrderRepository orderRepository,
            OrderStatePersistenceService orderStatePersistenceService,
            ProcessedEventService processedEventService,
            OrderService orderService, OrderOutboxService orderOutboxService
    ) {
        this.orderRepository =
                orderRepository;

        this.orderStatePersistenceService =
                orderStatePersistenceService;

        this.processedEventService =
                processedEventService;

        this.orderService =
                orderService;
        this.orderOutboxService = orderOutboxService;
    }

    @Transactional
    public boolean handlePaymentSucceeded(
            UUID eventId,
            PaymentSucceededEvent event
    ) {

        /*
         * Inbox / idempotency protection.
         *
         * Kafka provides at-least-once delivery, so the same
         * event may be delivered more than once.
         *
         * eventId comes from EventEnvelope.
         */
        boolean newlyProcessed =
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PAYMENT_SUCCEEDED",
                        event.paymentId()
                );

        /*
         * Duplicate event.
         *
         * The database already contains this eventId.
         * Nothing else should be processed.
         */
        if (!newlyProcessed) {
            return false;
        }

        /*
         * Load the order.
         */
        Order order =
                orderRepository.findById(
                                event.orderId()
                        )
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                event.orderId()
                                        )
                        );

        /*
         * Idempotent business handling.
         *
         * The event may arrive after the order has already
         * reached CONFIRMED.
         */
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return true;
        }

        /*
         * PAYMENT_SUCCEEDED is only valid while the order
         * is waiting for payment confirmation.
         */
        if (order.getStatus()
                != OrderStatus.PAYMENT_PENDING) {

            throw new IllegalStateException(
                    "Cannot process PAYMENT_SUCCEEDED for order "
                            + order.getId()
                            + " because current status is "
                            + order.getStatus()
            );
        }

        /*
         * Validate that the payment belongs to the same
         * customer who owns the order.
         */
        if (!order.getCustomerId().equals(
                event.customerId()
        )) {

            throw new IllegalStateException(
                    "Payment customer does not match order"
            );
        }

        /*
         * Validate the payment amount.
         */
        if (order.getTotalAmount().compareTo(
                event.amount()
        ) != 0) {

            throw new IllegalStateException(
                    "Payment amount does not match order"
            );
        }

        /*
         * Validate currency.
         */
        if (!order.getCurrency().equals(
                event.currency()
        )) {

            throw new IllegalStateException(
                    "Payment currency does not match order"
            );
        }

        /*
         * Payment is valid.
         *
         * confirmPayment() performs TWO database operations
         * inside the same transaction:
         *
         * 1. Order → CONFIRMED
         * 2. Create ORDER_CONFIRMED outbox event
         *
         * Kafka is NOT called here.
         */
        orderStatePersistenceService.confirmPayment(
                order,
                event.paymentReference()
        );

        return true;
    }
    /**
     * Handles PAYMENT_REFUNDED events.
     *
     * The refund event itself contains the business data,
     * while eventId comes from the Kafka EventEnvelope.
     */
    @Transactional
    public boolean handlePaymentRefunded(
            UUID eventId,
            PaymentRefundedEvent event
    ) {

        /*
         * Inbox / idempotency protection.
         *
         * Kafka can deliver the same event more than once.
         *
         * eventId comes from EventEnvelope.
         */
        boolean newlyProcessed =
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PAYMENT_REFUNDED",
                        event.refundId()
                );

        /*
         * Duplicate event.
         *
         * The event has already been processed successfully.
         */
        if (!newlyProcessed) {

            return false;
        }

        /*
         * Load the order and validate that the refund
         * belongs to this order.
         */
        Order order =
                orderRepository.findById(
                                event.orderId()
                        )
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                event.orderId()
                                        )
                        );

        /*
         * Validate customer.
         */
        if (!order.getCustomerId().equals(
                event.customerId()
        )) {

            throw new IllegalStateException(
                    "Refund customer does not match order"
            );
        }

        /*
         * Validate refund amount.
         */
        if (order.getTotalAmount().compareTo(
                event.amount()
        ) != 0) {

            throw new IllegalStateException(
                    "Refund amount does not match order"
            );
        }

        /*
         * Validate currency.
         */
        if (!order.getCurrency().equals(
                event.currency()
        )) {

            throw new IllegalStateException(
                    "Refund currency does not match order"
            );
        }

        /*
         * If cancellation has already completed,
         * this event is effectively idempotent.
         */
        if (order.getStatus() == OrderStatus.CANCELLED) {

            return true;
        }

        /*
         * A successful refund should only complete
         * cancellation for a CONFIRMED order.
         */
        if (order.getStatus()
                != OrderStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Cannot process PAYMENT_REFUNDED for order "
                            + order.getId()
                            + " because current status is "
                            + order.getStatus()
            );
        }

        /*
         * Payment has been refunded successfully.
         *
         * Complete the remaining cancellation Saga:
         *
         * CONFIRMED
         *     ↓
         * restore inventory
         *     ↓
         * CANCELLED
         *
         * The existing OrderService implementation performs
         * the inventory restoration using the deterministic
         * stock-restoration operation ID.
         */
        orderService.completeRefundedCancellation(
                event.orderId(),
                "Payment refunded: "
                        + event.refundReference()
        );

        return true;
    }
}
