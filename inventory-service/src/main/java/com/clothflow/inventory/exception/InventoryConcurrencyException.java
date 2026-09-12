package com.clothflow.inventory.exception;

public class InventoryConcurrencyException
        extends RuntimeException {

    public InventoryConcurrencyException() {
        super(
                "Inventory was modified by another request. " +
                        "Please retry the operation."
        );
    }
}