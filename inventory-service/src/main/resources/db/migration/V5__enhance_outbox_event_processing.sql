ALTER TABLE outbox_event
    DROP CONSTRAINT chk_outbox_event_status;

ALTER TABLE outbox_event
    ADD CONSTRAINT chk_outbox_event_status
    CHECK (
        status IN (
            'PENDING',
            'PROCESSING',
            'PUBLISHED'
        )
    );

ALTER TABLE outbox_event
    ADD COLUMN processing_started_at TIMESTAMP;

ALTER TABLE outbox_event
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE outbox_event
    ADD COLUMN last_error TEXT;