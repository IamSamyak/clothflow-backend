package com.clothflow.shipping.exception;

import com.clothflow.shipping.entity.ShipmentStatus;

public class InvalidShipmentStatusTransitionException
        extends RuntimeException {

    public InvalidShipmentStatusTransitionException(
            ShipmentStatus currentStatus,
            ShipmentStatus targetStatus
    ) {

        super(
                "Invalid shipment status transition: "
                        + currentStatus
                        + " -> "
                        + targetStatus
        );
    }
}