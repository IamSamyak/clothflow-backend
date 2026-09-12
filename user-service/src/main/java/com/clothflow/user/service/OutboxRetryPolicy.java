package com.clothflow.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OutboxRetryPolicy {

    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;

    public OutboxRetryPolicy(
            @Value("${outbox.publisher.max-retries:5}")
            int maxRetries,

            @Value("${outbox.publisher.initial-backoff-ms:1000}")
            long initialBackoffMs,

            @Value("${outbox.publisher.max-backoff-ms:30000}")
            long maxBackoffMs
    ) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    public boolean shouldRetry(
            int currentRetryCount
    ) {
        return currentRetryCount < maxRetries;
    }

    public Duration calculateBackoff(
            int retryCount
    ) {

        long multiplier =
                1L << Math.min(
                        retryCount,
                        30
                );

        long baseDelay =
                Math.min(
                        initialBackoffMs * multiplier,
                        maxBackoffMs
                );

        long jitter =
                (long) (
                        Math.random()
                                * Math.max(
                                1,
                                baseDelay / 4
                        )
                );

        return Duration.ofMillis(
                Math.min(
                        baseDelay + jitter,
                        maxBackoffMs
                )
        );
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}