DROP INDEX IF EXISTS idx_outbox_event_pending;

CREATE INDEX idx_outbox_event_pending
    ON outbox_event (status, next_attempt_at, created_at);