package com.clothflow.order.client;

import com.clothflow.order.entity.PaymentMethod;
import com.clothflow.order.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResult(

        UUID paymentId,

        String paymentReference,

        UUID orderId,

        BigDecimal amount,

        String currency,

        PaymentStatus status,

        PaymentMethod paymentMethod
) {
}