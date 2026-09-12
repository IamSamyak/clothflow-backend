package com.clothflow.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ServiceTokenRequest(

        @NotBlank
        String clientId,

        @NotBlank
        String clientSecret
) {
}