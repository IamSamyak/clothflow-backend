package com.clothflow.shipping.provider;

import com.clothflow.shipping.entity.ShipmentStatus;

public record TrackingInfo(
        String trackingNumber,
        ShipmentStatus status
) {
}