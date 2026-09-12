ALTER TABLE payments
ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE payments
ADD CONSTRAINT uk_payments_idempotency_key
UNIQUE (idempotency_key);

CREATE INDEX idx_payments_idempotency_key
    ON payments(idempotency_key);