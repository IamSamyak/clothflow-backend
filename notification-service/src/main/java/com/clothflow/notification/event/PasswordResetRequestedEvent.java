package com.clothflow.notification.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID eventId,
        UUID userId,
        String email,
        String resetToken
) {
}