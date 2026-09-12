package com.clothflow.order.client;

import java.util.UUID;

public interface InventoryClient {

    void reserve(
            UUID productId,
            UUID reservationId,
            int quantity
    );

    void release(
            UUID productId,
            UUID reservationId,
            int quantity
    );

    void commit(
            UUID productId,
            UUID reservationId,
            int quantity
    );

    void addStock(
            UUID productId,
            UUID operationId,
            int quantity
    );
}