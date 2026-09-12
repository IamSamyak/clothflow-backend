CREATE TABLE notifications (
    id UUID PRIMARY KEY,

    customer_id UUID NOT NULL,

    channel VARCHAR(30) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    subject VARCHAR(255),

    content TEXT NOT NULL,

    status VARCHAR(30) NOT NULL,

    retry_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMP WITH TIME ZONE,

    last_error TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    sent_at TIMESTAMP WITH TIME ZONE,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_notifications_channel
        CHECK (
            channel IN (
                'EMAIL',
                'SMS',
                'PUSH'
            )
        ),

    CONSTRAINT chk_notifications_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_notifications_retry_count
        CHECK (
            retry_count >= 0
        )
);


CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,

    event_type VARCHAR(100) NOT NULL,

    aggregate_type VARCHAR(100),

    aggregate_id UUID,

    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);


CREATE TABLE notification_delivery_attempt (
    id UUID PRIMARY KEY,

    notification_id UUID NOT NULL,

    attempt_number INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL,

    error_message TEXT,

    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_delivery_attempt_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_delivery_attempt_number
        CHECK (
            attempt_number > 0
        )
);


CREATE INDEX idx_notifications_customer_id
    ON notifications(customer_id);


CREATE INDEX idx_notifications_status_attempt
    ON notifications(status, next_attempt_at);


CREATE INDEX idx_notifications_event_type
    ON notifications(event_type);


CREATE INDEX idx_notifications_created_at
    ON notifications(created_at);


CREATE INDEX idx_processed_event_processed_at
    ON processed_event(processed_at);


CREATE INDEX idx_processed_event_aggregate
    ON processed_event(
        aggregate_type,
        aggregate_id
    );


CREATE INDEX idx_delivery_attempt_notification
    ON notification_delivery_attempt(
        notification_id
    );