package com.clothflow.notification.messaging;

import com.clothflow.notification.config.NotificationMetrics;
import com.clothflow.notification.event.EventEnvelope;
import com.clothflow.notification.event.OrderConfirmedEvent;
import com.clothflow.notification.event.OrderEventType;
import com.clothflow.notification.service.NotificationEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    private final NotificationEventService
            notificationEventService;

    private final NotificationMetrics
            notificationMetrics;

    public OrderEventConsumer(
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
            topics = "clothflow.order.events",
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
                    "Received order event: eventId={}, eventType={}, " +
                            "aggregateType={}, aggregateId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.aggregateType(),
                    envelope.aggregateId()
            );

            OrderEventType eventType;

            try {

                eventType =
                        OrderEventType.valueOf(
                                envelope.eventType()
                        );

            } catch (IllegalArgumentException ex) {

                throw new IllegalArgumentException(
                        "Unsupported order event type: "
                                + envelope.eventType(),
                        ex
                );
            }

            switch (eventType) {

                case ORDER_CONFIRMED -> {

                    OrderConfirmedEvent event =
                            objectMapper.treeToValue(
                                    envelope.payload(),
                                    OrderConfirmedEvent.class
                            );

                    notificationEventService
                            .handleOrderConfirmed(
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
                    "Order event acknowledged: eventId={}",
                    envelope.eventId()
            );

        } catch (Exception ex) {

            notificationMetrics.processingFailure();

            log.error(
                    "Failed to process order notification event",
                    ex
            );

            throw ex;
        }
    }
}