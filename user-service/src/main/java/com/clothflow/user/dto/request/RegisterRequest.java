package com.clothflow.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 3, max = 100)
        String username,

        @NotBlank
        @Size(min = 12, max = 128)
        String password
) {
}