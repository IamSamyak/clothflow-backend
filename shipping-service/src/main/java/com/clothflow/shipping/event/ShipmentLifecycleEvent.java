package com.clothflow.shipping.event;

import com.clothflow.shipping.entity.ShipmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentLifecycleEvent(
        UUID shipmentId,
        UUID orderId,
        UUID customerId,
        ShipmentStatus status,
        String carrier,
        String trackingNumber,
        OffsetDateTime occurredAt
) {
}