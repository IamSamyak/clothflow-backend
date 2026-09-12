package com.clothflow.order.event;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentLifecycleEvent(

        UUID shipmentId,

        UUID orderId,

        UUID customerId,

        String status,

        String carrier,

        String trackingNumber,

        OffsetDateTime occurredAt

) {}