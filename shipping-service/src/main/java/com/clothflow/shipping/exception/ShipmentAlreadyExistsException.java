package com.clothflow.shipping.exception;

import java.util.UUID;

public class ShipmentAlreadyExistsException
        extends RuntimeException {

    public ShipmentAlreadyExistsException(
            UUID orderId
    ) {

        super(
                "Shipment already exists for order: "
                        + orderId
        );
    }
}