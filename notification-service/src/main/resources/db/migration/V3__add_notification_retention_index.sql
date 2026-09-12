CREATE INDEX idx_notifications_retention
    ON notifications(event_type, created_at);