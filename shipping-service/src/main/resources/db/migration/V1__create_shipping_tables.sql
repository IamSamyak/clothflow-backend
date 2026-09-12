CREATE TABLE shipments (
    id UUID PRIMARY KEY,

    order_id UUID NOT NULL UNIQUE,

    customer_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    carrier VARCHAR(100),

    tracking_number VARCHAR(100),

    recipient_name VARCHAR(200) NOT NULL,

    address_line1 VARCHAR(255) NOT NULL,

    address_line2 VARCHAR(255),

    city VARCHAR(100) NOT NULL,

    state VARCHAR(100) NOT NULL,

    postal_code VARCHAR(20) NOT NULL,

    country VARCHAR(100) NOT NULL,

    shipped_at TIMESTAMP WITH TIME ZONE,

    delivered_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_shipments_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SHIPPED',
                'OUT_FOR_DELIVERY',
                'DELIVERED',
                'CANCELLED',
                'DELIVERY_FAILED'
            )
        )
);


CREATE TABLE shipment_items (
    id UUID PRIMARY KEY,

    shipment_id UUID NOT NULL,

    product_id UUID NOT NULL,

    product_name VARCHAR(255) NOT NULL,

    quantity INTEGER NOT NULL,

    CONSTRAINT fk_shipment_items_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_shipment_items_quantity
        CHECK (quantity > 0)
);


CREATE INDEX idx_shipments_customer_id
    ON shipments(customer_id);

CREATE INDEX idx_shipments_status
    ON shipments(status);

CREATE INDEX idx_shipments_tracking_number
    ON shipments(tracking_number);

CREATE INDEX idx_shipments_created_at
    ON shipments(created_at);

CREATE INDEX idx_shipment_items_shipment_id
    ON shipment_items(shipment_id);

CREATE INDEX idx_shipment_items_product_id
    ON shipment_items(product_id);