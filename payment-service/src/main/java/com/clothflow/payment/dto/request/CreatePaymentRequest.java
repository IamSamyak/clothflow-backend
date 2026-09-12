package com.clothflow.payment.dto.request;

import com.clothflow.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotNull
        UUID orderId,

        @NotNull
        UUID customerId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotNull
        PaymentMethod paymentMethod
) {
}