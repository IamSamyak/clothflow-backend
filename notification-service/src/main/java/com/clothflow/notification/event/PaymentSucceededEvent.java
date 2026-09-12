package com.clothflow.notification.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentSucceededEvent(

        UUID paymentId,

        UUID orderId,

        UUID customerId,

        BigDecimal amount,

        String currency,

        String paymentReference,

        String paymentMethod,

        OffsetDateTime occurredAt

) {
}