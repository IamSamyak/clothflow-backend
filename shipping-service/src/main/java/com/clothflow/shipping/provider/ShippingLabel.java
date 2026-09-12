package com.clothflow.shipping.provider;

public record ShippingLabel(
        String carrier,
        String trackingNumber
) {
}