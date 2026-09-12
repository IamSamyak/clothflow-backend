package com.clothflow.notification.event;

import java.util.UUID;

public record OrderConfirmedItem(

        UUID productId,

        String productName,

        Integer quantity

) {
}