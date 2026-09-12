package com.clothflow.shipping.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    public UUID getCurrentUserId(
            Authentication authentication
    ) {

        if (!(authentication
                instanceof JwtAuthenticationToken jwtAuthenticationToken)) {

            throw new IllegalStateException(
                    "Authenticated request does not contain a JWT"
            );
        }

        String subject =
                jwtAuthenticationToken
                        .getToken()
                        .getSubject();

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT subject is not a valid user UUID",
                    ex
            );
        }
    }

    public boolean isServiceToken(
            Authentication authentication
    ) {

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("TOKEN_SERVICE")
                );
    }
}