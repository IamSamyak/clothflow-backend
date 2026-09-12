package com.clothflow.shipping.exception;

import java.util.UUID;

public class ShipmentForOrderNotFoundException
        extends RuntimeException {

    public ShipmentForOrderNotFoundException(
            UUID orderId
    ) {
        super(
                "Shipment not found for order: "
                        + orderId
        );
    }
}