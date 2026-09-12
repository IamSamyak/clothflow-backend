package com.clothflow.order.dto.response;

import com.clothflow.order.entity.PaymentMethod;
import com.clothflow.order.entity.PaymentStatus;

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