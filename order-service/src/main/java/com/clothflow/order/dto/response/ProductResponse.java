package com.clothflow.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Long version
) {
}