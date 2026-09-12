package com.clothflow.order.event;

import java.util.UUID;

public record OrderConfirmedItem(

        UUID productId,

        String productName,

        Integer quantity
) {
}