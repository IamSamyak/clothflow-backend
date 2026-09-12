CREATE TABLE security_audit_events (
    id UUID PRIMARY KEY,

    user_id UUID,

    event_type VARCHAR(50) NOT NULL,

    outcome VARCHAR(20) NOT NULL,

    ip_address VARCHAR(45),

    user_agent VARCHAR(1000),

    correlation_id VARCHAR(100),

    details JSONB,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_security_audit_event_outcome
        CHECK (
            outcome IN ('SUCCESS', 'FAILURE')
        )
);

CREATE INDEX idx_security_audit_events_user_id
    ON security_audit_events(user_id);

CREATE INDEX idx_security_audit_events_event_type
    ON security_audit_events(event_type);

CREATE INDEX idx_security_audit_events_created_at
    ON security_audit_events(created_at);

CREATE INDEX idx_security_audit_events_user_created
    ON security_audit_events(user_id, created_at);