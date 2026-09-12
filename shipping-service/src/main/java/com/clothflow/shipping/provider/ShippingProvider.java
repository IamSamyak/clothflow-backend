package com.clothflow.shipping.provider;

import com.clothflow.shipping.entity.Shipment;

public interface ShippingProvider {

    ShippingLabel createShipment(
            Shipment shipment,
            String idempotencyKey
    );

    TrackingInfo getTrackingInfo(
            String trackingNumber
    );
}