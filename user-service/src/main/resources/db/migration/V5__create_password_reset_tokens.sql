CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    consumed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_password_reset_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);

CREATE INDEX idx_password_reset_tokens_expires_at
    ON password_reset_tokens(expires_at);

CREATE INDEX idx_password_reset_tokens_consumed_at
    ON password_reset_tokens(consumed_at);