package com.clothflow.order.exception;

import java.util.UUID;

public class InventoryInsufficientStockException
        extends RuntimeException {

    private final UUID productId;

    public InventoryInsufficientStockException(UUID productId) {
        super("Insufficient inventory for product: " + productId);
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }
}