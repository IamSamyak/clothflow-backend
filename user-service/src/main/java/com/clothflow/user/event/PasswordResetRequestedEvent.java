package com.clothflow.user.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID eventId,
        UUID userId,
        String email,
        String resetToken
) {
}