package com.clothflow.user.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PasswordResetRateLimitService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PasswordResetRateLimitService.class
            );

    private static final int IP_LIMIT = 10;

    private static final int EMAIL_LIMIT = 3;

    private static final Duration IP_WINDOW =
            Duration.ofMinutes(1);

    private static final Duration EMAIL_WINDOW =
            Duration.ofMinutes(15);

    private static final String KEY_PREFIX =
            "clothflow:rate-limit:password-reset:";

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

    public PasswordResetRateLimitService(
            StringRedisTemplate redisTemplate,
            RateLimitKeyHasher rateLimitKeyHasher,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.rateLimitKeyHasher = rateLimitKeyHasher;
        this.meterRegistry = meterRegistry;
    }

    public PasswordResetRateLimitDecision check(
            String ipAddress,
            String normalizedEmail
    ) {

        try {

            boolean ipAllowed =
                    consume(
                            KEY_PREFIX + "ip:" + ipAddress,
                            IP_LIMIT,
                            IP_WINDOW
                    );

            if (!ipAllowed) {
                return PasswordResetRateLimitDecision.IP_BLOCKED;
            }

            String emailKey =
                    rateLimitKeyHasher.hash(
                            normalizedEmail
                    );

            boolean emailAllowed =
                    consume(
                            KEY_PREFIX + "email:" + emailKey,
                            EMAIL_LIMIT,
                            EMAIL_WINDOW
                    );

            if (!emailAllowed) {
                return PasswordResetRateLimitDecision.EMAIL_BLOCKED;
            }

            return PasswordResetRateLimitDecision.ALLOWED;

        } catch (Exception ex) {

            meterRegistry
                    .counter(
                            "clothflow.password_reset.rate_limit.unavailable"
                    )
                    .increment();

            log.error(
                    "Password reset rate limiter unavailable; failing open",
                    ex
            );

            return PasswordResetRateLimitDecision.UNAVAILABLE;
        }
    }

    private boolean consume(
            String key,
            int limit,
            Duration window
    ) {

        Long count =
                redisTemplate.execute(
                        RATE_LIMIT_SCRIPT,
                        List.of(key),
                        String.valueOf(
                                window.toSeconds()
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