package com.clothflow.order.outbox;

import com.clothflow.order.entity.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisherService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OutboxPublisherService.class
            );

    private static final String ORDER_EVENTS_TOPIC =
            "clothflow.order.events";

    private static final int MAX_ERROR_LENGTH = 2000;

    private static final int MAX_RETRIES = 5;

    private final OutboxEventClaimService
            outboxEventClaimService;

    private final KafkaTemplate<String, String>
            kafkaTemplate;

    @Value(
            "${clothflow.outbox.retry-delay-seconds:10}"
    )
    private long retryDelaySeconds;

    public OutboxPublisherService(
            OutboxEventClaimService outboxEventClaimService,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventClaimService =
                outboxEventClaimService;

        this.kafkaTemplate =
                kafkaTemplate;
    }

    @Scheduled(
            fixedDelayString =
                    "${clothflow.outbox.poll-interval-ms:1000}"
    )
    public void publishPendingEvents() {

        /*
         * First recover events whose processing lease
         * expired because a previous publisher crashed.
         */
        outboxEventClaimService
                .recoverStaleProcessingEvents();

        /*
         * Claim pending events in a short DB transaction.
         *
         * FOR UPDATE SKIP LOCKED allows multiple
         * Order Service instances to safely compete
         * for outbox events.
         */
        List<OutboxEvent> events =
                outboxEventClaimService
                        .claimPendingEvents();

        if (events.isEmpty()) {
            return;
        }

        log.debug(
                "Claimed {} outbox events",
                events.size()
        );

        for (OutboxEvent event : events) {

            publishEvent(event);
        }
    }

    private void publishEvent(
            OutboxEvent event
    ) {

        try {

            kafkaTemplate
                    .send(
                            ORDER_EVENTS_TOPIC,
                            event.getAggregateId().toString(),
                            event.getPayload()
                    )
                    .get(
                            10,
                            TimeUnit.SECONDS
                    );

            outboxEventClaimService
                    .markPublished(event);

            log.info(
                    "Outbox event published successfully: " +
                            "eventId={}, eventType={}, aggregateId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId()
            );

        } catch (Exception ex) {

            handlePublicationFailure(
                    event,
                    ex
            );
        }
    }

    private void handlePublicationFailure(
            OutboxEvent event,
            Exception ex
    ) {

        int nextRetryCount =
                event.getRetryCount() + 1;

        String error =
                truncateError(
                        ex.getMessage()
                );

        if (nextRetryCount >= MAX_RETRIES) {

            outboxEventClaimService.markFailed(
                    event,
                    error
            );

            log.error(
                    "Outbox event permanently failed: " +
                            "eventId={}, eventType={}, aggregateId={}, " +
                            "retryCount={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    nextRetryCount,
                    ex
            );

            return;
        }

        long delaySeconds =
                calculateRetryDelay(
                        nextRetryCount
                );

        OffsetDateTime nextAttemptAt =
                OffsetDateTime.now()
                        .plusSeconds(delaySeconds);

        outboxEventClaimService.markForRetry(
                event,
                nextAttemptAt,
                error
        );

        log.warn(
                "Outbox event publication failed; " +
                        "scheduled for retry: " +
                        "eventId={}, eventType={}, aggregateId={}, " +
                        "retryCount={}, nextAttemptAt={}",
                event.getId(),
                event.getEventType(),
                event.getAggregateId(),
                nextRetryCount,
                nextAttemptAt,
                ex
        );
    }

    private long calculateRetryDelay(
            int retryCount
    ) {

        long multiplier =
                1L << Math.min(
                        retryCount - 1,
                        5
                );

        return Math.min(
                retryDelaySeconds * multiplier,
                300
        );
    }

    private String truncateError(
            String error
    ) {

        if (error == null) {
            return "Unknown Kafka publication error";
        }

        if (error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }

        return error.substring(
                0,
                MAX_ERROR_LENGTH
        );
    }
}