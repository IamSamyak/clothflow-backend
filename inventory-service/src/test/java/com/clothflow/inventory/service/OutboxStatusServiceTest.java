package com.clothflow.inventory.service;

import com.clothflow.inventory.entity.OutboxEvent;
import com.clothflow.inventory.entity.OutboxEventStatus;
import com.clothflow.inventory.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxStatusServiceTest {

    private OutboxEventRepository outboxEventRepository;

    private OutboxRetryPolicy retryPolicy;

    private OutboxStatusService outboxStatusService;

    @BeforeEach
    void setUp() {

        outboxEventRepository =
                mock(OutboxEventRepository.class);

        retryPolicy =
                mock(OutboxRetryPolicy.class);

        outboxStatusService =
                new OutboxStatusService(
                        outboxEventRepository,
                        retryPolicy
                );
    }

    @Test
    void shouldMarkEventAsPublished() {

        OutboxEvent event =
                new OutboxEvent();

        event.setStatus(
                OutboxEventStatus.PROCESSING
        );

        event.setProcessingStartedAt(
                LocalDateTime.now()
        );

        event.setRetryCount(2);

        event.setLastError(
                "Previous Kafka failure"
        );

        event.setNextAttemptAt(
                LocalDateTime.now()
        );

        outboxStatusService
                .markPublished(event);

        assertThat(event.getStatus())
                .isEqualTo(
                        OutboxEventStatus.PUBLISHED
                );

        assertThat(event.getPublishedAt())
                .isNotNull();

        assertThat(event.getProcessingStartedAt())
                .isNull();

        assertThat(event.getNextAttemptAt())
                .isNull();

        assertThat(event.getLastError())
                .isNull();

        /*
         * Successful publishing does not reset
         * retry_count. It remains useful for
         * historical/operational information.
         */
        assertThat(event.getRetryCount())
                .isEqualTo(2);

        verify(outboxEventRepository)
                .save(event);

        verifyNoInteractions(retryPolicy);
    }

    @Test
    void shouldReturnEventToPendingAfterFailure() {

        OutboxEvent event =
                new OutboxEvent();

        event.setStatus(
                OutboxEventStatus.PROCESSING
        );

        event.setProcessingStartedAt(
                LocalDateTime.now()
        );

        event.setRetryCount(2);

        event.setLastError(null);

        Duration retryDelay =
                Duration.ofSeconds(10);

        /*
         * Retry #3 is still allowed.
         */
        when(retryPolicy.shouldRetry(3))
                .thenReturn(true);

        /*
         * IMPORTANT:
         *
         * OutboxStatusService uses calculateDelayWithJitter(),
         * not calculateDelay().
         *
         * We mock the jittered delay so this unit test
         * remains deterministic.
         */
        when(retryPolicy.calculateDelayWithJitter(3))
                .thenReturn(retryDelay);

        LocalDateTime before =
                LocalDateTime.now();

        outboxStatusService
                .markFailed(
                        event,
                        "Kafka unavailable"
                );

        LocalDateTime after =
                LocalDateTime.now();

        assertThat(event.getStatus())
                .isEqualTo(
                        OutboxEventStatus.PENDING
                );

        assertThat(event.getProcessingStartedAt())
                .isNull();

        assertThat(event.getRetryCount())
                .isEqualTo(3);

        assertThat(event.getLastError())
                .isEqualTo(
                        "Kafka unavailable"
                );

        assertThat(event.getPublishedAt())
                .isNull();

        assertThat(event.getNextAttemptAt())
                .isNotNull();

        assertThat(event.getNextAttemptAt())
                .isBetween(
                        before.plus(retryDelay),
                        after.plus(retryDelay)
                );

        verify(retryPolicy)
                .shouldRetry(3);

        verify(retryPolicy)
                .calculateDelayWithJitter(3);

        verify(outboxEventRepository)
                .save(event);
    }

    @Test
    void shouldMarkEventAsFailedAfterMaximumAttempts() {

        OutboxEvent event =
                new OutboxEvent();

        event.setStatus(
                OutboxEventStatus.PROCESSING
        );

        event.setProcessingStartedAt(
                LocalDateTime.now()
        );

        event.setRetryCount(4);

        when(retryPolicy.shouldRetry(5))
                .thenReturn(false);

        outboxStatusService
                .markFailed(
                        event,
                        "Kafka permanently unavailable"
                );

        assertThat(event.getStatus())
                .isEqualTo(
                        OutboxEventStatus.FAILED
                );

        assertThat(event.getRetryCount())
                .isEqualTo(5);

        assertThat(event.getProcessingStartedAt())
                .isNull();

        assertThat(event.getNextAttemptAt())
                .isNull();

        assertThat(event.getLastError())
                .isEqualTo(
                        "Kafka permanently unavailable"
                );

        verify(retryPolicy)
                .shouldRetry(5);

        verify(retryPolicy, never())
                .calculateDelay(anyInt());

        verify(retryPolicy, never())
                .calculateDelayWithJitter(anyInt());

        verify(outboxEventRepository)
                .save(event);
    }
}