CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,

    event_type VARCHAR(100) NOT NULL,

    aggregate_type VARCHAR(100),

    aggregate_id UUID,

    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);


CREATE INDEX idx_processed_event_processed_at
    ON processed_event(processed_at);

CREATE INDEX idx_processed_event_aggregate
    ON processed_event(aggregate_type, aggregate_id);