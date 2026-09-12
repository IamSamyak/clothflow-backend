ALTER TABLE outbox_event
    ADD COLUMN next_attempt_at TIMESTAMP;

UPDATE outbox_event
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL;