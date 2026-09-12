package com.clothflow.user.dto.response;

import com.clothflow.user.entity.RoleName;
import com.clothflow.user.entity.UserStatus;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        UserStatus status,
        Set<RoleName> roles
) {
}