package com.clothflow.payment.outbox;

import com.clothflow.payment.entity.OutboxEvent;
import com.clothflow.payment.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OutboxEventStateService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventStateService(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository =
                outboxEventRepository;
    }

    @Transactional
    public void markPublished(
            UUID eventId
    ) {

        OutboxEvent event =
                outboxEventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Outbox event not found: "
                                                + eventId
                                )
                        );

        event.markPublished();

        outboxEventRepository.save(event);
    }

    @Transactional
    public void markForRetry(
            UUID eventId,
            OffsetDateTime nextAttemptAt,
            String error
    ) {

        OutboxEvent event =
                outboxEventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Outbox event not found: "
                                                + eventId
                                )
                        );

        event.markForRetry(
                nextAttemptAt,
                error
        );

        outboxEventRepository.save(event);
    }

    @Transactional
    public void recoverStaleEvents() {

        OffsetDateTime now =
                OffsetDateTime.now();

        var staleEvents =
                outboxEventRepository
                        .findStaleProcessingEventsForUpdate(
                                now
                        );

        for (OutboxEvent event : staleEvents) {

            event.recoverFromStaleProcessing();

            outboxEventRepository.save(event);
        }
    }
}