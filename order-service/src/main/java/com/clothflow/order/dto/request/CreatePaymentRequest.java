package com.clothflow.order.dto.request;

import com.clothflow.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        PaymentMethod paymentMethod
) {
}