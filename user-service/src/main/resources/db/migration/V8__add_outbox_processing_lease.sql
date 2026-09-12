ALTER TABLE outbox_events
    ADD COLUMN processing_started_at
        TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_outbox_events_processing_lease
    ON outbox_events(status, processing_started_at);