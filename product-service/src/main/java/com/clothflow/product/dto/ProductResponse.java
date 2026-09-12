package com.clothflow.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}