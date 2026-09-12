package com.clothflow.payment.dto.response;

import com.clothflow.payment.entity.PaymentRefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRefundResponse(

        UUID id,

        UUID paymentId,

        UUID orderId,

        String refundReference,

        BigDecimal amount,

        String currency,

        PaymentRefundStatus status,

        String idempotencyKey,

        Long version,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}