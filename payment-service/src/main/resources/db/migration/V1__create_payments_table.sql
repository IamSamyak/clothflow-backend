CREATE TABLE payments (
    id UUID PRIMARY KEY,

    payment_reference VARCHAR(50) NOT NULL UNIQUE,

    order_id UUID NOT NULL,

    customer_id UUID NOT NULL,

    amount NUMERIC(19, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL DEFAULT 'INR',

    status VARCHAR(30) NOT NULL,

    payment_method VARCHAR(30) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_payments_amount
        CHECK (amount > 0),

    CONSTRAINT chk_payments_status
        CHECK (
            status IN (
                'INITIATED',
                'PENDING',
                'SUCCEEDED',
                'FAILED',
                'CANCELLED',
                'REFUND_PENDING',
                'REFUNDED'
            )
        ),

    CONSTRAINT chk_payments_method
        CHECK (
            payment_method IN (
                'CARD',
                'UPI',
                'NET_BANKING',
                'COD'
            )
        )
);

CREATE INDEX idx_payments_order_id
    ON payments(order_id);

CREATE INDEX idx_payments_customer_id
    ON payments(customer_id);

CREATE INDEX idx_payments_status
    ON payments(status);

CREATE INDEX idx_payments_created_at
    ON payments(created_at);