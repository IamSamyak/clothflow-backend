package com.clothflow.shipping.dto.response;

import com.clothflow.shipping.entity.ShipmentStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(

        UUID id,

        UUID orderId,

        UUID customerId,

        ShipmentStatus status,

        String carrier,

        String trackingNumber,

        String recipientName,

        String addressLine1,

        String addressLine2,

        String city,

        String state,

        String postalCode,

        String country,

        OffsetDateTime shippedAt,

        OffsetDateTime deliveredAt,

        Long version,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt,

        List<ShipmentItemResponse> items
) {
}