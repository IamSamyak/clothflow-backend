package com.clothflow.order.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ServiceTokenProvider {

    private static final long REFRESH_SKEW_SECONDS = 30;

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    private final ReentrantLock refreshLock =
            new ReentrantLock();

    private volatile CachedToken cachedToken;

    public ServiceTokenProvider(
            RestClient.Builder restClientBuilder,
            @Value("${services.user.base-url}")
            String userServiceBaseUrl,
            @Value("${service-auth.client-id}")
            String clientId,
            @Value("${service-auth.client-secret}")
            String clientSecret
    ) {
        this.restClient =
                restClientBuilder
                        .baseUrl(userServiceBaseUrl)
                        .build();

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {

        CachedToken current = cachedToken;

        if (current != null && !current.isExpiringSoon()) {
            return current.accessToken();
        }

        return refreshToken();
    }

    private String refreshToken() {

        refreshLock.lock();

        try {

            CachedToken current = cachedToken;

            /*
             * Another thread may have refreshed the token
             * while this thread was waiting for the lock.
             */
            if (current != null
                    && !current.isExpiringSoon()) {

                return current.accessToken();
            }

            ServiceTokenResponse response =
                    restClient
                            .post()
                            .uri("/api/v1/auth/service-token")
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(
                                    new ServiceTokenRequest(
                                            clientId,
                                            clientSecret
                                    )
                            )
                            .retrieve()
                            .body(ServiceTokenResponse.class);

            if (response == null
                    || response.accessToken() == null
                    || response.accessToken().isBlank()) {

                throw new IllegalStateException(
                        "User Service returned an invalid service token"
                );
            }

            if (response.expiresIn() <= 0) {

                throw new IllegalStateException(
                        "User Service returned an invalid token expiration"
                );
            }

            Instant expiresAt =
                    Instant.now()
                            .plusSeconds(response.expiresIn());

            cachedToken =
                    new CachedToken(
                            response.accessToken(),
                            expiresAt
                    );

            return response.accessToken();

        } finally {
            refreshLock.unlock();
        }
    }

    private record ServiceTokenRequest(
            String clientId,
            String clientSecret
    ) {
    }

    private record ServiceTokenResponse(
            @JsonProperty("accessToken")
            String accessToken,

            @JsonProperty("expiresIn")
            long expiresIn
    ) {
    }

    private record CachedToken(
            String accessToken,
            Instant expiresAt
    ) {

        boolean isExpiringSoon() {

            return Instant.now()
                    .plusSeconds(REFRESH_SKEW_SECONDS)
                    .isAfter(expiresAt);
        }
    }
}