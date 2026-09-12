CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    published_at TIMESTAMP,

    CONSTRAINT chk_outbox_event_status
        CHECK (
            status IN (
                'PENDING',
                'PUBLISHED'
            )
        )
);

CREATE INDEX idx_outbox_event_pending
    ON outbox_event (status, created_at);

CREATE INDEX idx_outbox_event_aggregate
    ON outbox_event (aggregate_type, aggregate_id);