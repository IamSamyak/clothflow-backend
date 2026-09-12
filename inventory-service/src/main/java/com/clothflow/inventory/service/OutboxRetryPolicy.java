package com.clothflow.inventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OutboxRetryPolicy {

    private final long baseDelaySeconds;
    private final long maxDelaySeconds;
    private final int maxAttempts;

    public OutboxRetryPolicy(
            @Value("${clothflow.outbox.retry.base-delay-seconds}")
            long baseDelaySeconds,

            @Value("${clothflow.outbox.retry.max-delay-seconds}")
            long maxDelaySeconds,

            @Value("${clothflow.outbox.retry.max-attempts}")
            int maxAttempts) {

        this.baseDelaySeconds = baseDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
        this.maxAttempts = maxAttempts;
    }

    public Duration calculateDelay(
            int retryCount) {

        long multiplier =
                1L << Math.min(
                        Math.max(retryCount - 1, 0),
                        30
                );

        long delay =
                baseDelaySeconds * multiplier;

        return Duration.ofSeconds(
                Math.min(
                        delay,
                        maxDelaySeconds
                )
        );
    }

    public Duration calculateDelayWithJitter(
            int retryCount) {

        Duration baseDelay =
                calculateDelay(retryCount);

        long baseSeconds =
                baseDelay.getSeconds();

        if (baseSeconds == 0) {
            return Duration.ZERO;
        }

        long jitter =
                ThreadLocalRandom.current()
                        .nextLong(
                                0,
                                baseSeconds + 1
                        );

        return Duration.ofSeconds(
                jitter
        );
    }

    public boolean shouldRetry(
            int retryCount) {

        return retryCount < maxAttempts;
    }
}