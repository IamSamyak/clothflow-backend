package com.clothflow.inventory.exception;

public class InvalidInventoryOperationException
        extends RuntimeException {

    public InvalidInventoryOperationException(String message) {
        super(message);
    }

    public InvalidInventoryOperationException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}