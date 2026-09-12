package com.clothflow.shipping.outbox;

import com.clothflow.shipping.entity.OutboxEvent;
import com.clothflow.shipping.entity.OutboxEventStatus;
import com.clothflow.shipping.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OutboxPublisherService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OutboxPublisherService.class
            );

    private static final int MAX_RETRIES = 10;

    private final OutboxClaimService outboxClaimService;

    private final OutboxEventPublisher eventPublisher;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisherService(
            OutboxClaimService outboxClaimService,
            OutboxEventPublisher eventPublisher,
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxClaimService =
                outboxClaimService;

        this.eventPublisher =
                eventPublisher;

        this.outboxEventRepository =
                outboxEventRepository;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxClaimService
                        .claimPendingEvents();

        if (events.isEmpty()) {
            return;
        }

        log.info(
                "Claimed {} outbox events for publishing",
                events.size()
        );

        for (OutboxEvent event : events) {

            publish(event);
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void recoverStaleEvents() {

        outboxClaimService
                .recoverStaleProcessingEvents();
    }

    private void publish(
            OutboxEvent event
    ) {

        try {

            log.info(
                    "Publishing outbox event: " +
                            "id={}, type={}, aggregateId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId()
            );

            eventPublisher.publish(event);

            event.markPublished();

            outboxEventRepository.save(
                    event
            );

            log.info(
                    "Outbox event published successfully: {}",
                    event.getId()
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to publish outbox event: {}",
                    event.getId(),
                    ex
            );

            handleFailure(
                    event,
                    ex
            );
        }
    }

    private void handleFailure(
            OutboxEvent event,
            Exception ex
    ) {

        int nextRetryCount =
                event.getRetryCount() + 1;

        if (nextRetryCount > MAX_RETRIES) {

            event.setStatus(
                    OutboxEventStatus.FAILED
            );

            event.setLastError(
                    ex.getMessage()
            );

            event.setNextAttemptAt(
                    null
            );

            outboxEventRepository.save(
                    event
            );

            log.error(
                    "Outbox event permanently failed after " +
                            "{} retries: id={}",
                    MAX_RETRIES,
                    event.getId()
            );

            return;
        }

        OffsetDateTime nextAttemptAt =
                calculateNextAttempt(
                        nextRetryCount
                );

        event.markForRetry(
                nextAttemptAt,
                ex.getMessage()
        );

        outboxEventRepository.save(
                event
        );

        log.warn(
                "Outbox event scheduled for retry: " +
                        "id={}, retryCount={}, " +
                        "nextAttemptAt={}",
                event.getId(),
                nextRetryCount,
                nextAttemptAt
        );
    }

    private OffsetDateTime calculateNextAttempt(
            int retryCount
    ) {

        long exponentialDelay =
                Math.min(
                        300,
                        1L << Math.min(
                                retryCount,
                                8
                        )
                );

        long jitter =
                ThreadLocalRandom.current()
                        .nextLong(
                                0,
                                Math.max(
                                        1,
                                        exponentialDelay / 2
                                )
                        );

        return OffsetDateTime.now()
                .plusSeconds(
                        exponentialDelay + jitter
                );
    }
}