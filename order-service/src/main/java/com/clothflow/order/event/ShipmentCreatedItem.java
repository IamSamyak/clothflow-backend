package com.clothflow.order.event;

import java.util.UUID;

public record ShipmentCreatedItem(
        UUID productId,
        String productName,
        Integer quantity
) {
}