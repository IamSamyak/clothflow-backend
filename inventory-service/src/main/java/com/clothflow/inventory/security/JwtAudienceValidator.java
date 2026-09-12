package com.clothflow.inventory.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class JwtAudienceValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String CUSTOMER_AUDIENCE =
            "clothflow-api";

    private static final String INTERNAL_AUDIENCE =
            "clothflow-internal";

    private static final OAuth2Error INVALID_AUDIENCE =
            new OAuth2Error(
                    "invalid_token",
                    "Token audience is invalid",
                    null
            );

    @Override
    public OAuth2TokenValidatorResult validate(
            Jwt jwt
    ) {

        List<String> audiences =
                jwt.getAudience();

        if (audiences == null) {
            return OAuth2TokenValidatorResult.failure(
                    INVALID_AUDIENCE
            );
        }

        boolean valid =
                audiences.contains(CUSTOMER_AUDIENCE)
                        || audiences.contains(
                        INTERNAL_AUDIENCE
                );

        if (!valid) {
            return OAuth2TokenValidatorResult.failure(
                    INVALID_AUDIENCE
            );
        }

        return OAuth2TokenValidatorResult.success();
    }
}