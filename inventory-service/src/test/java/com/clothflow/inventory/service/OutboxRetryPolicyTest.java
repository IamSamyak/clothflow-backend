package com.clothflow.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRetryPolicyTest {

    private OutboxRetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {

        retryPolicy =
                new OutboxRetryPolicy(
                        5L,
                        300L,
                        5
                );
    }

    @Test
    void shouldCalculateExponentialBackoff() {

        assertThat(
                retryPolicy.calculateDelay(1)
        ).isEqualTo(
                Duration.ofSeconds(5)
        );

        assertThat(
                retryPolicy.calculateDelay(2)
        ).isEqualTo(
                Duration.ofSeconds(10)
        );

        assertThat(
                retryPolicy.calculateDelay(3)
        ).isEqualTo(
                Duration.ofSeconds(20)
        );

        assertThat(
                retryPolicy.calculateDelay(4)
        ).isEqualTo(
                Duration.ofSeconds(40)
        );
    }

    @Test
    void shouldRespectMaximumDelay() {

        assertThat(
                retryPolicy.calculateDelay(10)
        ).isEqualTo(
                Duration.ofSeconds(300)
        );

        assertThat(
                retryPolicy.calculateDelay(20)
        ).isEqualTo(
                Duration.ofSeconds(300)
        );
    }

    @Test
    void shouldAllowRetryBeforeMaximumAttempts() {

        assertThat(
                retryPolicy.shouldRetry(1)
        ).isTrue();

        assertThat(
                retryPolicy.shouldRetry(2)
        ).isTrue();

        assertThat(
                retryPolicy.shouldRetry(4)
        ).isTrue();
    }

    @Test
    void shouldRejectRetryAtMaximumAttempts() {

        assertThat(
                retryPolicy.shouldRetry(5)
        ).isFalse();
    }

    @Test
    void shouldRejectRetryAfterMaximumAttempts() {

        assertThat(
                retryPolicy.shouldRetry(6)
        ).isFalse();

        assertThat(
                retryPolicy.shouldRetry(10)
        ).isFalse();
    }

    @Test
    void shouldCalculateJitterWithinExpectedRange() {

        Duration delay =
                retryPolicy.calculateDelayWithJitter(3);

        assertThat(delay)
                .isGreaterThanOrEqualTo(
                        Duration.ZERO
                );

        assertThat(delay)
                .isLessThanOrEqualTo(
                        Duration.ofSeconds(20)
                );
    }
}