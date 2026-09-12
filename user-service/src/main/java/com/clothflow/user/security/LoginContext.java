package com.clothflow.user.security;

public record LoginContext(
        String clientIp,
        String userAgent,
        String correlationId
) {
}