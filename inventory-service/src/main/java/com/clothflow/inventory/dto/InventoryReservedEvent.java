package com.clothflow.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        UUID reservationId,
        UUID productId,
        Long quantity,
        LocalDateTime occurredAt
) {
}