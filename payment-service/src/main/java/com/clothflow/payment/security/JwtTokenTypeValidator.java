package com.clothflow.payment.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtTokenTypeValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN_TYPE =
            new OAuth2Error(
                    "invalid_token",
                    "Invalid JWT token type",
                    null
            );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String tokenType =
                jwt.getClaimAsString("token_type");

        if ("access".equals(tokenType)
                || "service".equals(tokenType)) {

            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(
                INVALID_TOKEN_TYPE
        );
    }
}