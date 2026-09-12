package com.clothflow.order.event;

import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(

        UUID orderId,

        UUID customerId,

        String recipientName,

        String addressLine1,

        String addressLine2,

        String city,

        String state,

        String postalCode,

        String country,

        List<OrderConfirmedItem> items
) {
}