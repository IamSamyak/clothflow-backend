CREATE TABLE order_inventory_reservations (
    id UUID PRIMARY KEY,

    order_id UUID NOT NULL,

    order_item_id UUID NOT NULL,

    reservation_id UUID NOT NULL,

    product_id UUID NOT NULL,

    quantity INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_order_inventory_reservation_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_inventory_reservation_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_order_inventory_reservation_id
        UNIQUE (reservation_id),

    CONSTRAINT uk_order_inventory_order_item
        UNIQUE (order_item_id),

    CONSTRAINT chk_order_inventory_reservation_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_inventory_reservation_status
        CHECK (
            status IN (
                'PENDING',
                'RESERVED',
                'RELEASED',
                'FAILED',
                'RELEASE_FAILED'
            )
        )
);

CREATE INDEX idx_order_inventory_reservation_order_id
    ON order_inventory_reservations(order_id);

CREATE INDEX idx_order_inventory_reservation_product_id
    ON order_inventory_reservations(product_id);

CREATE INDEX idx_order_inventory_reservation_status
    ON order_inventory_reservations(status);

CREATE INDEX idx_order_inventory_reservation_reservation_id
    ON order_inventory_reservations(reservation_id);