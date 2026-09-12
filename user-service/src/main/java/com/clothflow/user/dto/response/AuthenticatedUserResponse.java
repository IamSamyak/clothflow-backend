package com.clothflow.user.dto.response;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID userId,
        List<String> roles
) {
}