package com.clothflow.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        Long quantity,
        Long reservedQuantity,
        Long availableQuantity,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}