package com.clothflow.user.security;

import com.clothflow.user.entity.SecurityAuditEventType;
import com.clothflow.user.entity.SecurityAuditOutcome;

import java.util.UUID;

public record SecurityAuditCommand(
        UUID userId,
        SecurityAuditEventType eventType,
        SecurityAuditOutcome outcome,
        String ipAddress,
        String userAgent,
        String correlationId,
        String details
) {
}