package com.clothflow.user.service;

import com.clothflow.user.dto.request.ServiceTokenRequest;
import com.clothflow.user.dto.response.ServiceTokenResponse;
import com.clothflow.user.entity.ServiceClient;
import com.clothflow.user.exception.AuthenticationFailedException;
import com.clothflow.user.repository.ServiceClientRepository;
import com.clothflow.user.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceTokenService {

    private final ServiceClientRepository serviceClientRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final long expirationSeconds;

    public ServiceTokenService(
            ServiceClientRepository serviceClientRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${jwt.service-token-expiration}")
            long expirationSeconds
    ) {
        this.serviceClientRepository =
                serviceClientRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.jwtService =
                jwtService;

        this.expirationSeconds =
                expirationSeconds;
    }

    @Transactional(readOnly = true)
    public ServiceTokenResponse issueToken(
            ServiceTokenRequest request
    ) {

        ServiceClient client =
                serviceClientRepository
                        .findByClientId(
                                request.clientId()
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Invalid service credentials"
                                )
                        );

        if (!client.isEnabled()) {
            throw new AuthenticationFailedException(
                    "Invalid service credentials"
            );
        }

        boolean secretMatches =
                passwordEncoder.matches(
                        request.clientSecret(),
                        client.getClientSecretHash()
                );

        if (!secretMatches) {
            throw new AuthenticationFailedException(
                    "Invalid service credentials"
            );
        }

        /*
         * Scopes are server-controlled.
         *
         * They are NOT accepted from the HTTP request.
         */
        List<String> scopes =
                scopesFor(
                        client.getServiceName()
                );

        String token =
                jwtService.generateServiceToken(
                        client.getServiceName(),
                        scopes,
                        expirationSeconds
                );

        return new ServiceTokenResponse(
                token,
                expirationSeconds
        );
    }

    private List<String> scopesFor(
            String serviceName
    ) {

        return switch (serviceName) {

            case "order-service" ->
                    List.of(
                            "inventory.read",
                            "inventory.write",
                            "payment.read",
                            "payment.write"
                    );

            case "shipping-service" ->
                    List.of(
                            "order.read",
                            "order.shipping.write"
                    );

            default ->
                    List.of();
        };
    }
}