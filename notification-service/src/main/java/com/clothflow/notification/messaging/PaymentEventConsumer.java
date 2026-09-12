package com.clothflow.notification.messaging;

import com.clothflow.notification.config.NotificationMetrics;
import com.clothflow.notification.event.EventEnvelope;
import com.clothflow.notification.event.PaymentEventType;
import com.clothflow.notification.event.PaymentRefundedEvent;
import com.clothflow.notification.event.PaymentSucceededEvent;
import com.clothflow.notification.service.NotificationEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    private final NotificationEventService
            notificationEventService;

    private final NotificationMetrics
            notificationMetrics;

    public PaymentEventConsumer(
            ObjectMapper objectMapper,
            NotificationEventService notificationEventService,
            NotificationMetrics notificationMetrics
    ) {
        this.objectMapper = objectMapper;

        this.notificationEventService =
                notificationEventService;

        this.notificationMetrics =
                notificationMetrics;
    }

    @KafkaListener(
            topics = "clothflow.payment.events",
            groupId = "clothflow-notification-service"
    )
    public void consume(
            String payload,
            Acknowledgment acknowledgment
    ) throws Exception {

        notificationMetrics.notificationReceived();

        try {

            EventEnvelope<JsonNode> envelope =
                    objectMapper.readValue(
                            payload,
                            objectMapper.getTypeFactory()
                                    .constructParametricType(
                                            EventEnvelope.class,
                                            JsonNode.class
                                    )
                    );

            log.info(
                    "Received payment event: eventId={}, eventType={}, " +
                            "aggregateType={}, aggregateId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.aggregateType(),
                    envelope.aggregateId()
            );

            PaymentEventType eventType;

            try {

                eventType =
                        PaymentEventType.valueOf(
                                envelope.eventType()
                        );

            } catch (IllegalArgumentException ex) {

                throw new IllegalArgumentException(
                        "Unsupported payment event type: "
                                + envelope.eventType(),
                        ex
                );
            }

            switch (eventType) {

                case PAYMENT_SUCCEEDED -> {

                    PaymentSucceededEvent event =
                            objectMapper.treeToValue(
                                    envelope.payload(),
                                    PaymentSucceededEvent.class
                            );

                    notificationEventService
                            .handlePaymentSucceeded(
                                    envelope.eventId(),
                                    envelope.eventType(),
                                    envelope.aggregateType(),
                                    envelope.aggregateId(),
                                    event
                            );
                }

                case PAYMENT_REFUNDED -> {

                    PaymentRefundedEvent event =
                            objectMapper.treeToValue(
                                    envelope.payload(),
                                    PaymentRefundedEvent.class
                            );

                    notificationEventService
                            .handlePaymentRefunded(
                                    envelope.eventId(),
                                    envelope.eventType(),
                                    envelope.aggregateType(),
                                    envelope.aggregateId(),
                                    event
                            );
                }
            }

            acknowledgment.acknowledge();

            log.info(
                    "Payment event acknowledged: eventId={}",
                    envelope.eventId()
            );

        } catch (Exception ex) {

            notificationMetrics.processingFailure();

            log.error(
                    "Failed to process payment notification event",
                    ex
            );

            throw ex;
        }
    }
}