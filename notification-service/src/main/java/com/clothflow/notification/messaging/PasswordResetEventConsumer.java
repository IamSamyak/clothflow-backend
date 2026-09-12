package com.clothflow.notification.messaging;

import com.clothflow.notification.config.NotificationMetrics;
import com.clothflow.notification.event.PasswordResetRequestedEvent;
import com.clothflow.notification.service.NotificationEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class PasswordResetEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PasswordResetEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    private final NotificationEventService
            notificationEventService;

    private final NotificationMetrics
            notificationMetrics;

    public PasswordResetEventConsumer(
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
            topics = "clothflow.user.password-reset-requested",
            groupId = "clothflow-notification-service"
    )
    public void consume(
            String payload,
            Acknowledgment acknowledgment,
            org.apache.kafka.clients.consumer.ConsumerRecord<
                    String,
                    String
                    > record
    ) throws Exception {

        notificationMetrics.notificationReceived();

        try {

            String eventIdHeader =
                    headerValue(
                            record,
                            "event-id"
                    );

            String eventTypeHeader =
                    headerValue(
                            record,
                            "event-type"
                    );

            if (eventIdHeader == null) {

                throw new IllegalArgumentException(
                        "Missing Kafka event-id header"
                );
            }

            if (eventTypeHeader == null) {

                throw new IllegalArgumentException(
                        "Missing Kafka event-type header"
                );
            }

            UUID eventId;

            try {

                eventId =
                        UUID.fromString(
                                eventIdHeader
                        );

            } catch (IllegalArgumentException exception) {

                throw new IllegalArgumentException(
                        "Invalid Kafka event-id header",
                        exception
                );
            }

            PasswordResetRequestedEvent event =
                    objectMapper.readValue(
                            payload,
                            PasswordResetRequestedEvent.class
                    );

            if (!eventId.equals(event.eventId())) {

                throw new IllegalArgumentException(
                        "Kafka event-id does not match payload eventId"
                );
            }

            log.info(
                    "Received password reset event: " +
                            "eventId={}, eventType={}, userId={}",
                    eventId,
                    eventTypeHeader,
                    event.userId()
            );

            notificationEventService
                    .handlePasswordResetRequested(
                            eventId,
                            eventTypeHeader,
                            "USER",
                            event.userId(),
                            event
                    );

            acknowledgment.acknowledge();

            log.info(
                    "Password reset event acknowledged: eventId={}",
                    eventId
            );

        } catch (Exception ex) {

            notificationMetrics.processingFailure();

            log.error(
                    "Failed to process password reset notification event",
                    ex
            );

            throw ex;
        }
    }

    private String headerValue(
            org.apache.kafka.clients.consumer.ConsumerRecord<
                    String,
                    String
                    > record,
            String name
    ) {

        Header header =
                record.headers()
                        .lastHeader(name);

        if (header == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}