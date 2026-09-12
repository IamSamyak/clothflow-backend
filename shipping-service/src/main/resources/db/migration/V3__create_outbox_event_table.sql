CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(30) NOT NULL,

    retry_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMP WITH TIME ZONE,

    last_error TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    processed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_outbox_event_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PUBLISHED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_outbox_event_status_attempt
    ON outbox_event(status, next_attempt_at);

CREATE INDEX idx_outbox_event_aggregate
    ON outbox_event(aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_event_created_at
    ON outbox_event(created_at);