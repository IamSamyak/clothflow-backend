package com.clothflow.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 12, max = 128)
        String newPassword
) {
}