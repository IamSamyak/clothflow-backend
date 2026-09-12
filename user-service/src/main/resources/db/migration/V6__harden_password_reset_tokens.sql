ALTER TABLE password_reset_tokens
    ADD COLUMN invalidated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_password_reset_tokens_active
    ON password_reset_tokens(user_id, expires_at)
    WHERE consumed_at IS NULL
      AND invalidated_at IS NULL;