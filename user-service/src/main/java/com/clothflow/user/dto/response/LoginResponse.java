package com.clothflow.user.dto.response;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}