CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,

    customer_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    total_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_orders_total_amount
        CHECK (total_amount >= 0),

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                'PENDING',
                'INVENTORY_RESERVED',
                'PAYMENT_PENDING',
                'CONFIRMED',
                'PROCESSING',
                'SHIPPED',
                'DELIVERED',
                'CANCELLED',
                'INVENTORY_FAILED',
                'PAYMENT_FAILED'
            )
        )
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,

    order_id UUID NOT NULL,

    product_id UUID NOT NULL,

    sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,

    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal NUMERIC(19, 2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_order_items_subtotal
        CHECK (subtotal >= 0)
);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY,

    order_id UUID NOT NULL,

    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,

    reason VARCHAR(500),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_orders_customer_id
    ON orders(customer_id);

CREATE INDEX idx_orders_status
    ON orders(status);

CREATE INDEX idx_orders_created_at
    ON orders(created_at);

CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);

CREATE INDEX idx_order_items_product_id
    ON order_items(product_id);

CREATE INDEX idx_order_status_history_order_id
    ON order_status_history(order_id);

CREATE INDEX idx_order_status_history_created_at
    ON order_status_history(created_at);