package com.clothflow.user.dto.response;

public record ServiceTokenResponse(
        String accessToken,
        long expiresIn
) {
}