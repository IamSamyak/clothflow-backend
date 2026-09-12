package com.clothflow.order.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID refundId,
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String refundReference
) {
}