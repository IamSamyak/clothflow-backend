ALTER TABLE refresh_tokens
    ADD COLUMN replaced_by UUID;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_replaced_by
    FOREIGN KEY (replaced_by)
    REFERENCES refresh_tokens(id);

CREATE INDEX idx_refresh_tokens_replaced_by
    ON refresh_tokens(replaced_by);

ALTER TABLE users
    ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;