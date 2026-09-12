package com.clothflow.shipping.outbox;

import com.clothflow.shipping.entity.OutboxEvent;
import com.clothflow.shipping.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxClaimService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OutboxClaimService.class
            );

    private static final long PROCESSING_LEASE_SECONDS = 30;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxClaimService(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository =
                outboxEventRepository;
    }

    @Transactional
    public List<OutboxEvent> claimPendingEvents() {

        OffsetDateTime now =
                OffsetDateTime.now();

        List<OutboxEvent> events =
                outboxEventRepository
                        .findPendingEventsForUpdate(now);

        if (events.isEmpty()) {
            return events;
        }

        OffsetDateTime leaseUntil =
                now.plusSeconds(
                        PROCESSING_LEASE_SECONDS
                );

        for (OutboxEvent event : events) {

            event.markProcessing(
                    leaseUntil
            );
        }

        return events;
    }

    @Transactional
    public void recoverStaleProcessingEvents() {

        OffsetDateTime now =
                OffsetDateTime.now();

        List<OutboxEvent> events =
                outboxEventRepository
                        .findStaleProcessingEventsForUpdate(
                                now
                        );

        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {

            log.warn(
                    "Recovering stale outbox event: " +
                            "id={}, type={}, " +
                            "aggregateId={}, retryCount={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getRetryCount()
            );

            event.recoverFromStaleProcessing();
        }

        log.info(
                "Recovered {} stale outbox events",
                events.size()
        );
    }
}