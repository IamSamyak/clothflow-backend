CREATE TABLE processed_webhook (
    provider_event_id VARCHAR(255) PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,
    event_type VARCHAR(100),
    tracking_number VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_webhook_processed_at
    ON processed_webhook(processed_at);

CREATE INDEX idx_processed_webhook_tracking_number
    ON processed_webhook(tracking_number);