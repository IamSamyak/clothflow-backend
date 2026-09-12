package com.clothflow.user.service;

import com.clothflow.user.security.RateLimitKeyHasher;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class LoginRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitService.class);

    private static final int IP_LIMIT = 100;

    private static final int EMAIL_LIMIT = 10;

    private static final Duration WINDOW =
            Duration.ofMinutes(1);

    private static final String KEY_PREFIX =
            "clothflow:rate-limit:login:";

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

    public LoginRateLimitService(
            StringRedisTemplate redisTemplate,
            RateLimitKeyHasher rateLimitKeyHasher, MeterRegistry meterRegistry
    ) {
        this.redisTemplate =
                redisTemplate;

        this.rateLimitKeyHasher =
                rateLimitKeyHasher;
        this.meterRegistry = meterRegistry;
    }

    public RateLimitDecision check(
            String ipAddress,
            String normalizedEmail
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

                return RateLimitDecision.IP_BLOCKED;
            }

            String emailKey =
                    rateLimitKeyHasher.hash(
                            normalizedEmail
                    );

            boolean emailAllowed =
                    consume(
                            KEY_PREFIX
                                    + "email:"
                                    + emailKey,
                            EMAIL_LIMIT
                    );

            if (!emailAllowed) {

                return RateLimitDecision.EMAIL_BLOCKED;
            }

            return RateLimitDecision.ALLOWED;

        } catch (Exception ex) {
            meterRegistry.counter("clothflow.login.rate_limit.unavailable").increment();
            log.error("Login rate limiter unavailable; failing open", ex);
            return RateLimitDecision.UNAVAILABLE;
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