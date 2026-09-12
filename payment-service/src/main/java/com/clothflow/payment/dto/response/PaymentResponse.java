package com.clothflow.payment.dto.response;

import com.clothflow.payment.entity.PaymentMethod;
import com.clothflow.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(

        UUID id,

        String paymentReference,

        String idempotencyKey,

        UUID orderId,

        UUID customerId,

        BigDecimal amount,

        String currency,

        PaymentStatus status,

        PaymentMethod paymentMethod,

        Long version,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}