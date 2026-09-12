package com.clothflow.inventory.service;

import com.clothflow.inventory.entity.OutboxEvent;
import com.clothflow.inventory.entity.OutboxEventStatus;
import com.clothflow.inventory.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OutboxStatusService {

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxRetryPolicy retryPolicy;

    public OutboxStatusService(
            OutboxEventRepository outboxEventRepository,
            OutboxRetryPolicy retryPolicy) {

        this.outboxEventRepository =
                outboxEventRepository;

        this.retryPolicy =
                retryPolicy;
    }

    @Transactional
    public void markPublished(
            OutboxEvent event) {

        event.setStatus(
                OutboxEventStatus.PUBLISHED
        );

        event.setPublishedAt(
                LocalDateTime.now()
        );

        event.setProcessingStartedAt(
                null
        );

        event.setNextAttemptAt(
                null
        );

        event.setLastError(
                null
        );

        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(
            OutboxEvent event,
            String error) {

        int nextRetryCount =
                event.getRetryCount() + 1;

        event.setRetryCount(
                nextRetryCount
        );

        event.setLastError(
                error
        );

        event.setProcessingStartedAt(
                null
        );

        if (retryPolicy.shouldRetry(nextRetryCount)) {

            event.setStatus(
                    OutboxEventStatus.PENDING
            );

            event.setNextAttemptAt(
                    LocalDateTime.now()
                            .plus(
                                    retryPolicy.calculateDelayWithJitter(
                                            nextRetryCount
                                    )
                            )
            );

        } else {

            event.setStatus(
                    OutboxEventStatus.FAILED
            );

            event.setNextAttemptAt(
                    null
            );
        }

        outboxEventRepository.save(event);
    }
}