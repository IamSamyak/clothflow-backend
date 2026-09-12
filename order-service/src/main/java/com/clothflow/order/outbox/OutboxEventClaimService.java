package com.clothflow.order.outbox;

import com.clothflow.order.entity.OutboxEvent;
import com.clothflow.order.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxEventClaimService {

    @Value("${clothflow.outbox.lease-seconds:60}")
    private long leaseSeconds;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventClaimService(
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

        OffsetDateTime leaseUntil =
                now.plusSeconds(leaseSeconds);

        for (OutboxEvent event : events) {

            event.markProcessing(
                    leaseUntil
            );
        }

        outboxEventRepository.flush();

        return events;
    }

    @Transactional
    public void markPublished(
            OutboxEvent event
    ) {

        event.markPublished();

        outboxEventRepository.save(
                event
        );
    }

    @Transactional
    public void markForRetry(
            OutboxEvent event,
            OffsetDateTime nextAttemptAt,
            String error
    ) {

        event.markForRetry(
                nextAttemptAt,
                error
        );

        outboxEventRepository.save(
                event
        );
    }

    @Transactional
    public void recoverStaleProcessingEvents() {

        OffsetDateTime now =
                OffsetDateTime.now();

        List<OutboxEvent> staleEvents =
                outboxEventRepository
                        .findStaleProcessingEventsForUpdate(
                                now
                        );

        for (OutboxEvent event : staleEvents) {

            event.recoverFromStaleProcessing();
        }

        if (!staleEvents.isEmpty()) {
            outboxEventRepository.flush();
        }
    }

    @Transactional
    public void markFailed(
            OutboxEvent event,
            String error
    ) {

        event.markFailed(error);

        outboxEventRepository.save(event);
    }
}