CREATE TABLE inventory_stock_operation (
    id UUID PRIMARY KEY,

    operation_id UUID NOT NULL UNIQUE,

    inventory_id UUID NOT NULL,

    operation_type VARCHAR(20) NOT NULL,

    quantity BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_operation_inventory
        FOREIGN KEY (inventory_id)
        REFERENCES inventory(id),

    CONSTRAINT chk_stock_operation_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_stock_operation_type
        CHECK (
            operation_type IN (
                'ADD',
                'REMOVE'
            )
        )
);

CREATE INDEX idx_stock_operation_inventory_id
    ON inventory_stock_operation (inventory_id);