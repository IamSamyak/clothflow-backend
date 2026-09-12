CREATE TABLE payment_refund (
    id UUID PRIMARY KEY,

    payment_id UUID NOT NULL,

    order_id UUID NOT NULL,

    refund_reference VARCHAR(50) NOT NULL,

    amount NUMERIC(19, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    status VARCHAR(30) NOT NULL,

    idempotency_key VARCHAR(100) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_payment_refund_payment
        UNIQUE (payment_id),

    CONSTRAINT uk_payment_refund_reference
        UNIQUE (refund_reference),

    CONSTRAINT uk_payment_refund_idempotency
        UNIQUE (idempotency_key),

    CONSTRAINT chk_payment_refund_amount
        CHECK (amount > 0),

    CONSTRAINT chk_payment_refund_status
        CHECK (
            status IN (
                'PENDING',
                'SUCCEEDED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_payment_refund_order
    ON payment_refund(order_id);

CREATE INDEX idx_payment_refund_payment
    ON payment_refund(payment_id);

CREATE INDEX idx_payment_refund_status
    ON payment_refund(status);

CREATE INDEX idx_payment_refund_created_at
    ON payment_refund(created_at);