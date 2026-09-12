ALTER TABLE notification_delivery_attempt
    ADD CONSTRAINT chk_delivery_attempt_status
    CHECK (
        status IN (
            'STARTED',
            'SENT',
            'FAILED',
            'ABANDONED'
        )
    );