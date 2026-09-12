package com.clothflow.shipping.dto.response;

import java.util.UUID;

public record ShipmentItemResponse(

        UUID id,

        UUID productId,

        String productName,

        Integer quantity
) {
}