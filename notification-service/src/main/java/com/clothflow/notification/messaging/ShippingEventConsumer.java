package com.clothflow.notification.messaging;

import com.clothflow.notification.config.NotificationMetrics;
import com.clothflow.notification.event.EventEnvelope;
import com.clothflow.notification.event.ShipmentLifecycleEvent;
import com.clothflow.notification.event.ShippingEventType;
import com.clothflow.notification.service.NotificationEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ShippingEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ShippingEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    private final NotificationEventService
            notificationEventService;

    private final NotificationMetrics
            notificationMetrics;

    public ShippingEventConsumer(
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
            topics = "clothflow.shipping.events",
            groupId = "clothflow-notification-service"
    )
    public void consume(
            String payload,
            Acknowledgment acknowledgment
    ) throws Exception {

        /*
         * Kafka-level metric.
         *
         * This counts every delivery attempt that reaches
         * the consumer, including retries.
         */
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
                    "Received shipping event: eventId={}, eventType={}, " +
                            "aggregateType={}, aggregateId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.aggregateType(),
                    envelope.aggregateId()
            );

            ShippingEventType eventType;

            try {

                eventType =
                        ShippingEventType.valueOf(
                                envelope.eventType()
                        );

            } catch (IllegalArgumentException ex) {

                throw new IllegalArgumentException(
                        "Unsupported shipping event type: "
                                + envelope.eventType(),
                        ex
                );
            }

            switch (eventType) {

                case SHIPMENT_SHIPPED,
                     SHIPMENT_OUT_FOR_DELIVERY,
                     SHIPMENT_DELIVERED,
                     SHIPMENT_DELIVERY_FAILED -> {

                    ShipmentLifecycleEvent event =
                            objectMapper.treeToValue(
                                    envelope.payload(),
                                    ShipmentLifecycleEvent.class
                            );

                    notificationEventService
                            .handleShipmentLifecycle(
                                    envelope.eventId(),
                                    envelope.eventType(),
                                    envelope.aggregateType(),
                                    envelope.aggregateId(),
                                    event
                            );
                }
            }

            /*
             * Acknowledge only after the complete notification
             * processing path succeeds.
             */
            acknowledgment.acknowledge();

            log.info(
                    "Shipping event acknowledged: eventId={}",
                    envelope.eventId()
            );

        } catch (Exception ex) {

            /*
             * Do not acknowledge the message.
             *
             * The exception is rethrown so Spring Kafka's existing
             * retry / DLT mechanism can handle the failure.
             */
            notificationMetrics.processingFailure();

            log.error(
                    "Failed to process shipping notification event",
                    ex
            );

            throw ex;
        }
    }
}