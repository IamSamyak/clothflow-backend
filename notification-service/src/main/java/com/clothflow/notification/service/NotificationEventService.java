package com.clothflow.notification.service;

import com.clothflow.notification.config.NotificationMetrics;
import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationChannel;
import com.clothflow.notification.event.*;
import com.clothflow.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationEventService {

    private final ProcessedEventService processedEventService;

    private final NotificationRepository notificationRepository;

    private final NotificationMetrics notificationMetrics;

    public NotificationEventService(
            ProcessedEventService processedEventService,
            NotificationRepository notificationRepository,
            NotificationMetrics notificationMetrics
    ) {
        this.processedEventService =
                processedEventService;

        this.notificationRepository =
                notificationRepository;

        this.notificationMetrics =
                notificationMetrics;
    }

    @Transactional
    public boolean handleOrderConfirmed(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            OrderConfirmedEvent event
    ) {

        Timer.Sample processingTimer =
                notificationMetrics.startProcessingTimer();

        try {

            boolean newlyProcessed =
                    processedEventService.tryMarkProcessed(
                            eventId,
                            eventType,
                            aggregateType,
                            aggregateId
                    );

            if (!newlyProcessed) {

                notificationMetrics.duplicateEvent();

                return false;
            }

            validateOrderEvent(
                    aggregateId,
                    event.orderId(),
                    event.customerId()
            );

            Notification notification =
                    new Notification(
                            event.customerId(),
                            NotificationChannel.EMAIL,
                            eventType,
                            "Your ClothFlow order is confirmed",
                            buildOrderConfirmedContent(event)
                    );

            notificationRepository.save(
                    notification
            );

            notificationMetrics.notificationCreated();

            return true;

        } finally {

            notificationMetrics.recordProcessing(
                    processingTimer
            );
        }
    }

    @Transactional
    public boolean handlePaymentSucceeded(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            PaymentSucceededEvent event
    ) {

        Timer.Sample processingTimer =
                notificationMetrics.startProcessingTimer();

        try {

            boolean newlyProcessed =
                    processedEventService.tryMarkProcessed(
                            eventId,
                            eventType,
                            aggregateType,
                            aggregateId
                    );

            if (!newlyProcessed) {

                notificationMetrics.duplicateEvent();

                return false;
            }

            validatePaymentEvent(
                    aggregateId,
                    event.paymentId(),
                    event.customerId()
            );

            Notification notification =
                    new Notification(
                            event.customerId(),
                            NotificationChannel.EMAIL,
                            eventType,
                            "Payment received for your ClothFlow order",
                            buildPaymentSucceededContent(event)
                    );

            notificationRepository.save(
                    notification
            );

            notificationMetrics.notificationCreated();

            return true;

        } finally {

            notificationMetrics.recordProcessing(
                    processingTimer
            );
        }
    }

    @Transactional
    public boolean handlePaymentRefunded(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            PaymentRefundedEvent event
    ) {

        Timer.Sample processingTimer =
                notificationMetrics.startProcessingTimer();

        try {

            boolean newlyProcessed =
                    processedEventService.tryMarkProcessed(
                            eventId,
                            eventType,
                            aggregateType,
                            aggregateId
                    );

            if (!newlyProcessed) {

                notificationMetrics.duplicateEvent();

                return false;
            }

            validatePaymentEvent(
                    aggregateId,
                    event.paymentId(),
                    event.customerId()
            );

            Notification notification =
                    new Notification(
                            event.customerId(),
                            NotificationChannel.EMAIL,
                            eventType,
                            "Your ClothFlow payment has been refunded",
                            buildPaymentRefundedContent(event)
                    );

            notificationRepository.save(
                    notification
            );

            notificationMetrics.notificationCreated();

            return true;

        } finally {

            notificationMetrics.recordProcessing(
                    processingTimer
            );
        }
    }

    @Transactional
    public boolean handleShipmentLifecycle(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            ShipmentLifecycleEvent event
    ) {

        Timer.Sample processingTimer =
                notificationMetrics.startProcessingTimer();

        try {

            boolean firstTime =
                    processedEventService.tryMarkProcessed(
                            eventId,
                            eventType,
                            "SHIPMENT",
                            aggregateId
                    );

            if (!firstTime) {

                notificationMetrics.duplicateEvent();

                return false;
            }

            if (!"SHIPMENT".equals(aggregateType)) {

                throw new IllegalStateException(
                        "Shipment event must have aggregateType SHIPMENT"
                );
            }

            if (!aggregateId.equals(event.shipmentId())) {

                throw new IllegalStateException(
                        "Shipment event aggregateId does not match shipmentId"
                );
            }

            if (event.customerId() == null) {

                throw new IllegalStateException(
                        "Shipment event customerId must not be null"
                );
            }

            Notification notification =
                    new Notification(
                            event.customerId(),
                            NotificationChannel.EMAIL,
                            eventType,
                            shipmentSubject(event.status()),
                            shipmentContent(event)
                    );

            notificationRepository.save(
                    notification
            );

            notificationMetrics.notificationCreated();

            return true;

        } finally {

            notificationMetrics.recordProcessing(
                    processingTimer
            );
        }
    }

    private String shipmentSubject(
            String status
    ) {

        return switch (status) {

            case "SHIPPED" ->
                    "Your ClothFlow order has been shipped";

            case "OUT_FOR_DELIVERY" ->
                    "Your ClothFlow order is out for delivery";

            case "DELIVERED" ->
                    "Your ClothFlow order has been delivered";

            case "DELIVERY_FAILED" ->
                    "Delivery of your ClothFlow order failed";

            default ->
                    "Update on your ClothFlow shipment";
        };
    }

    private String shipmentContent(
            ShipmentLifecycleEvent event
    ) {

        return switch (event.status()) {

            case "SHIPPED" ->
                    """
                    Your ClothFlow order has been shipped.
    
                    Tracking number: %s
                    Carrier: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.carrier()
                    );

            case "OUT_FOR_DELIVERY" ->
                    """
                    Your ClothFlow order is out for delivery.
    
                    Tracking number: %s
                    Carrier: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.carrier()
                    );

            case "DELIVERED" ->
                    """
                    Your ClothFlow order has been delivered.
    
                    Tracking number: %s
                    Carrier: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.carrier()
                    );

            case "DELIVERY_FAILED" ->
                    """
                    We were unable to deliver your ClothFlow order.
    
                    Tracking number: %s
                    Carrier: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.carrier()
                    );

            default ->
                    """
                    There is an update regarding your ClothFlow shipment.
    
                    Tracking number: %s
                    Carrier: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.carrier()
                    );
        };
    }

    private void validateOrderEvent(
            UUID aggregateId,
            UUID orderId,
            UUID customerId
    ) {

        if (!aggregateId.equals(orderId)) {

            throw new IllegalStateException(
                    "Order event aggregateId does not match orderId"
            );
        }

        if (customerId == null) {

            throw new IllegalStateException(
                    "Customer ID is required"
            );
        }
    }

    private void validatePaymentEvent(
            UUID aggregateId,
            UUID paymentId,
            UUID customerId
    ) {

        if (!aggregateId.equals(paymentId)) {

            throw new IllegalStateException(
                    "Payment event aggregateId does not match paymentId"
            );
        }

        if (customerId == null) {

            throw new IllegalStateException(
                    "Customer ID is required"
            );
        }
    }

    private void validateShipmentEvent(
            UUID aggregateId,
            UUID shipmentId,
            UUID customerId
    ) {

        if (!aggregateId.equals(shipmentId)) {

            throw new IllegalStateException(
                    "Shipment event aggregateId does not match shipmentId"
            );
        }

        if (customerId == null) {

            throw new IllegalStateException(
                    "Customer ID is required"
            );
        }
    }

    private String buildOrderConfirmedContent(
            OrderConfirmedEvent event
    ) {

        return """
                Hello %s,

                Your ClothFlow order has been confirmed.

                Order ID: %s

                Your order is now being prepared for shipment.

                Thank you for shopping with ClothFlow.
                """.formatted(
                event.recipientName(),
                event.orderId()
        );
    }

    private String buildPaymentSucceededContent(
            PaymentSucceededEvent event
    ) {

        return """
                Your payment for ClothFlow order %s was successful.

                Amount: %s %s

                Payment reference: %s

                Thank you for your purchase.
                """.formatted(
                event.orderId(),
                event.amount(),
                event.currency(),
                event.paymentReference()
        );
    }

    private String buildPaymentRefundedContent(
            PaymentRefundedEvent event
    ) {

        return """
                Your payment for ClothFlow order %s has been refunded.

                Refund amount: %s %s

                Refund reference: %s
                """.formatted(
                event.orderId(),
                event.amount(),
                event.currency(),
                event.refundReference()
        );
    }

    private String buildShipmentSubject(
            ShipmentLifecycleEvent event
    ) {

        return switch (event.status()) {

            case "SHIPPED" ->
                    "Your ClothFlow order has been shipped";

            case "OUT_FOR_DELIVERY" ->
                    "Your ClothFlow order is out for delivery";

            case "DELIVERED" ->
                    "Your ClothFlow order has been delivered";

            case "DELIVERY_FAILED" ->
                    "Delivery update for your ClothFlow order";

            default ->
                    "Update for your ClothFlow order";
        };
    }

    private String buildShipmentContent(
            ShipmentLifecycleEvent event
    ) {

        return switch (event.status()) {

            case "SHIPPED" ->
                    """
                    Your ClothFlow order has been shipped.

                    Carrier: %s
                    Tracking number: %s

                    Order ID: %s
                    """.formatted(
                            event.carrier(),
                            event.trackingNumber(),
                            event.orderId()
                    );

            case "OUT_FOR_DELIVERY" ->
                    """
                    Your ClothFlow order is out for delivery.

                    Tracking number: %s

                    Order ID: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.orderId()
                    );

            case "DELIVERED" ->
                    """
                    Your ClothFlow order has been delivered.

                    Tracking number: %s

                    Order ID: %s

                    Thank you for shopping with ClothFlow.
                    """.formatted(
                            event.trackingNumber(),
                            event.orderId()
                    );

            case "DELIVERY_FAILED" ->
                    """
                    There was an issue delivering your ClothFlow order.

                    Tracking number: %s

                    Order ID: %s

                    Please contact support if you need assistance.
                    """.formatted(
                            event.trackingNumber(),
                            event.orderId()
                    );

            default ->
                    """
                    There is an update for your ClothFlow shipment.

                    Tracking number: %s

                    Order ID: %s
                    """.formatted(
                            event.trackingNumber(),
                            event.orderId()
                    );
        };
    }

    @Transactional
    public boolean handlePasswordResetRequested(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            PasswordResetRequestedEvent event
    ) {

        Timer.Sample processingTimer =
                notificationMetrics.startProcessingTimer();

        try {

            boolean newlyProcessed =
                    processedEventService.tryMarkProcessed(
                            eventId,
                            eventType,
                            aggregateType,
                            aggregateId
                    );

            if (!newlyProcessed) {

                notificationMetrics.duplicateEvent();

                return false;
            }

            validatePasswordResetEvent(
                    eventId,
                    aggregateId,
                    event
            );

            /*
             * Notification currently uses customerId as the
             * owning user identifier.
             *
             * For password reset, User Service's userId is
             * therefore stored in customer_id.
             *
             * We will revisit the naming/domain model when
             * User/Customer identity is integrated across
             * the platform.
             */
            Notification notification =
                    new Notification(
                            event.userId(),
                            NotificationChannel.EMAIL,
                            eventType,
                            "Reset your ClothFlow password",
                            buildPasswordResetContent(event)
                    );

            notificationRepository.save(
                    notification
            );

            notificationMetrics.notificationCreated();

            return true;

        } finally {

            notificationMetrics.recordProcessing(
                    processingTimer
            );
        }
    }

    private void validatePasswordResetEvent(
            UUID eventId,
            UUID aggregateId,
            PasswordResetRequestedEvent event
    ) {

        if (eventId == null) {

            throw new IllegalStateException(
                    "Password reset eventId must not be null"
            );
        }

        if (event.userId() == null) {

            throw new IllegalStateException(
                    "Password reset userId must not be null"
            );
        }

        if (!eventId.equals(event.eventId())) {

            throw new IllegalStateException(
                    "Password reset eventId does not match payload eventId"
            );
        }

        if (!aggregateId.equals(event.userId())) {

            throw new IllegalStateException(
                    "Password reset aggregateId does not match userId"
            );
        }

        if (event.email() == null || event.email().isBlank()) {

            throw new IllegalStateException(
                    "Password reset email must not be blank"
            );
        }

        if (event.resetToken() == null ||
                event.resetToken().isBlank()) {

            throw new IllegalStateException(
                    "Password reset token must not be blank"
            );
        }
    }

    private String buildPasswordResetContent(
            PasswordResetRequestedEvent event
    ) {

        return """
            Hello,

            We received a request to reset your ClothFlow password.

            Use the following reset token to continue:

            %s

            This token is temporary and can only be used once.

            If you did not request a password reset, you can safely ignore this message.

            ClothFlow Security
            """.formatted(
                event.resetToken()
        );
    }
}