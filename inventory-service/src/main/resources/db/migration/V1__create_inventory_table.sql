CREATE TABLE inventory (
    id UUID PRIMARY KEY,

    product_id UUID NOT NULL UNIQUE,

    quantity BIGINT NOT NULL DEFAULT 0,

    reserved_quantity BIGINT NOT NULL DEFAULT 0,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_inventory_quantity_non_negative
        CHECK (quantity >= 0),

    CONSTRAINT chk_inventory_reserved_non_negative
        CHECK (reserved_quantity >= 0),

    CONSTRAINT chk_inventory_reserved_not_greater_than_quantity
        CHECK (reserved_quantity <= quantity)
);