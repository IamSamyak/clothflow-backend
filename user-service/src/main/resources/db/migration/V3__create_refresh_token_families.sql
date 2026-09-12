CREATE TABLE refresh_token_families (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    compromised_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_refresh_token_families_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_refresh_token_family_status
        CHECK (
            status IN (
                'ACTIVE',
                'COMPROMISED',
                'REVOKED'
            )
        )
);

CREATE INDEX idx_refresh_token_families_user_id
    ON refresh_token_families(user_id);

CREATE INDEX idx_refresh_token_families_status
    ON refresh_token_families(status);

CREATE INDEX idx_refresh_token_families_created_at
    ON refresh_token_families(created_at);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_family
    FOREIGN KEY (family_id)
    REFERENCES refresh_token_families(id);