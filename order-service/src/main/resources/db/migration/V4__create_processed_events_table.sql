CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_event_aggregate
    ON processed_event(aggregate_id);

CREATE INDEX idx_processed_event_type
    ON processed_event(event_type);