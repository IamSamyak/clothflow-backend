ALTER TABLE outbox_event
    DROP CONSTRAINT chk_outbox_event_status;

ALTER TABLE outbox_event
    ADD CONSTRAINT chk_outbox_event_status
    CHECK (
        status IN (
            'PENDING',
            'PROCESSING',
            'PUBLISHED',
            'FAILED'
        )
    );