CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    published_at TIMESTAMP WITH TIME ZONE,

    retry_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMP WITH TIME ZONE,

    last_error TEXT,

    CONSTRAINT chk_outbox_events_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PUBLISHED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events(status, next_attempt_at, created_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events(aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_events_created_at
    ON outbox_events(created_at);