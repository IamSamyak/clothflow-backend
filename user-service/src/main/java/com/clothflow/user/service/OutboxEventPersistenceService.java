package com.clothflow.user.service;

import com.clothflow.user.entity.OutboxEvent;
import com.clothflow.user.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OutboxEventPersistenceService {

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxRetryPolicy retryPolicy;

    public OutboxEventPersistenceService(
            OutboxEventRepository outboxEventRepository, OutboxRetryPolicy retryPolicy
    ) {
        this.outboxEventRepository =
                outboxEventRepository;
        this.retryPolicy = retryPolicy;
    }

    @Transactional
    public void markPublished(
            java.util.UUID eventId
    ) {

        OutboxEvent event =
                outboxEventRepository.findById(
                                eventId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Outbox event not found: "
                                                + eventId
                                )
                        );

        event.markPublished();

        outboxEventRepository.save(
                event
        );
    }

    @Transactional
    public void handlePublishFailure(
            UUID eventId,
            String error
    ) {

        OutboxEvent event =
                outboxEventRepository.findById(
                                eventId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Outbox event not found: "
                                                + eventId
                                )
                        );

        if (event.getRetryCount()
                >= retryPolicy.getMaxRetries()) {

            event.markFailed(error);

            outboxEventRepository.save(event);

            return;
        }

        Duration backoff =
                retryPolicy.calculateBackoff(
                        event.getRetryCount()
                );

        event.markRetry(
                truncateError(error),
                OffsetDateTime.now()
                        .plus(backoff)
        );

        outboxEventRepository.save(event);
    }

    private String truncateError(
            String error
    ) {

        if (error == null) {
            return null;
        }

        return error.length() <= 2000
                ? error
                : error.substring(0, 2000);
    }
}