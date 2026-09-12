CREATE TABLE login_attempts (
    user_id UUID PRIMARY KEY,

    failed_attempts INTEGER NOT NULL DEFAULT 0,

    first_failed_at TIMESTAMP WITH TIME ZONE,

    last_failed_at TIMESTAMP WITH TIME ZONE,

    locked_until TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_login_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_login_attempts_failed_attempts
        CHECK (failed_attempts >= 0)
);

CREATE INDEX idx_login_attempts_locked_until
    ON login_attempts(locked_until);

CREATE INDEX idx_login_attempts_last_failed_at
    ON login_attempts(last_failed_at);