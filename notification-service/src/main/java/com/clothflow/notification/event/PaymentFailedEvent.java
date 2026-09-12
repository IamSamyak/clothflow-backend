package com.clothflow.notification.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(

        UUID paymentId,

        UUID orderId,

        UUID customerId,

        BigDecimal amount,

        String currency,

        String reason

) {
}