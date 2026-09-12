package com.clothflow.payment.dto;

import java.time.OffsetDateTime;

public record ApiErrorResponse(

        OffsetDateTime timestamp,

        int status,

        String error,

        String message,

        String path
) {
}