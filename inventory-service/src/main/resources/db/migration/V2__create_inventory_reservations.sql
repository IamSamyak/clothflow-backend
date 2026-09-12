CREATE TABLE inventory_reservation (
    id UUID PRIMARY KEY,

    reservation_id UUID NOT NULL UNIQUE,

    inventory_id UUID NOT NULL,

    quantity BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservation_inventory
        FOREIGN KEY (inventory_id)
        REFERENCES inventory(id),

    CONSTRAINT chk_reservation_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_reservation_status
        CHECK (
            status IN (
                'ACTIVE',
                'RELEASED',
                'COMMITTED'
            )
        )
);

CREATE INDEX idx_inventory_reservation_inventory_id
    ON inventory_reservation (inventory_id);

CREATE INDEX idx_inventory_reservation_status
    ON inventory_reservation (status);