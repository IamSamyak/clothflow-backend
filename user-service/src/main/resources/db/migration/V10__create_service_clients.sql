CREATE TABLE service_clients (
    id UUID PRIMARY KEY,

    client_id VARCHAR(100) NOT NULL UNIQUE,

    client_secret_hash VARCHAR(255) NOT NULL,

    service_name VARCHAR(100) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_clients_service_name
        CHECK (service_name IN (
            'order-service',
            'payment-service',
            'shipping-service'
        ))
);

CREATE INDEX idx_service_clients_client_id
    ON service_clients (client_id);