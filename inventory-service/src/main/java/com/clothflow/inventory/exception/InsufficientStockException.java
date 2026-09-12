package com.clothflow.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(
            long requested,
            long available) {

        super(
                "Insufficient stock. Requested: "
                        + requested
                        + ", Available: "
                        + available
        );
    }
}