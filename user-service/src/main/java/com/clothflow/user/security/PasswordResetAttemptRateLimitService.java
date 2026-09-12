package com.clothflow.user.security;

import com.clothflow.user.service.PasswordResetAttemptRateLimitDecision;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PasswordResetAttemptRateLimitService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PasswordResetAttemptRateLimitService.class
            );

    /*
     * A reset-token attempt is much more security-sensitive
     * than a normal login request.
     *
     * Keep the limit intentionally low.
     */
    private static final int IP_LIMIT = 30;

    private static final int TOKEN_LIMIT = 10;

    private static final Duration WINDOW =
            Duration.ofMinutes(1);

    private static final String KEY_PREFIX =
            "clothflow:rate-limit:password-reset-attempt:";

    private static final DefaultRedisScript<Long>
            RATE_LIMIT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call(
                        'INCR',
                        KEYS[1]
                    )

                    if count == 1 then
                        redis.call(
                            'EXPIRE',
                            KEYS[1],
                            ARGV[1]
                        )
                    end

                    return count
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final RateLimitKeyHasher rateLimitKeyHasher;
    private final MeterRegistry meterRegistry;

    public PasswordResetAttemptRateLimitService(
            StringRedisTemplate redisTemplate,
            RateLimitKeyHasher rateLimitKeyHasher,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.rateLimitKeyHasher = rateLimitKeyHasher;
        this.meterRegistry = meterRegistry;
    }

    public PasswordResetAttemptRateLimitDecision check(
            String ipAddress,
            String rawToken
    ) {

        try {

            String ipKey =
                    rateLimitKeyHasher.hash(ipAddress);

            boolean ipAllowed =
                    consume(
                            KEY_PREFIX + "ip:" + ipKey,
                            IP_LIMIT
                    );

            if (!ipAllowed) {
                return PasswordResetAttemptRateLimitDecision
                        .IP_BLOCKED;
            }

            /*
             * Never place the raw reset token in Redis.
             *
             * Hash it before constructing the Redis key.
             */
            String tokenKey =
                    rateLimitKeyHasher.hash(rawToken);

            boolean tokenAllowed =
                    consume(
                            KEY_PREFIX + "token:" + tokenKey,
                            TOKEN_LIMIT
                    );

            if (!tokenAllowed) {
                return PasswordResetAttemptRateLimitDecision
                        .TOKEN_BLOCKED;
            }

            return PasswordResetAttemptRateLimitDecision
                    .ALLOWED;

        } catch (Exception ex) {

            meterRegistry.counter(
                    "clothflow.password_reset_attempt.rate_limit.unavailable"
            ).increment();

            log.error(
                    "Password reset attempt rate limiter unavailable; failing open",
                    ex
            );

            return PasswordResetAttemptRateLimitDecision
                    .UNAVAILABLE;
        }
    }

    private boolean consume(
            String key,
            int limit
    ) {

        Long count =
                redisTemplate.execute(
                        RATE_LIMIT_SCRIPT,
                        List.of(key),
                        String.valueOf(
                                WINDOW.toSeconds()
                        )
                );

        if (count == null) {
            throw new IllegalStateException(
                    "Redis returned no rate-limit result"
            );
        }

        return count <= limit;
    }
}