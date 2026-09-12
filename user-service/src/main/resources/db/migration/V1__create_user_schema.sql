CREATE TABLE users (
    id UUID PRIMARY KEY,

    email VARCHAR(320) NOT NULL,

    username VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_email
        UNIQUE (email),

    CONSTRAINT uk_users_username
        UNIQUE (username),

    CONSTRAINT chk_users_status
        CHECK (
            status IN (
                'ACTIVE',
                'LOCKED',
                'DISABLED'
            )
        )
);


CREATE TABLE user_credentials (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_user_credentials_user
        UNIQUE (user_id),

    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


CREATE TABLE roles (
    id UUID PRIMARY KEY,

    name VARCHAR(50) NOT NULL,

    CONSTRAINT uk_roles_name
        UNIQUE (name)
);


CREATE TABLE user_roles (
    user_id UUID NOT NULL,

    role_id UUID NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);


CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    family_id UUID NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    revoked_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    last_used_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_refresh_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);


CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens(family_id);


CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);


CREATE INDEX idx_users_status
    ON users(status);

INSERT INTO roles (id, name)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'CUSTOMER'),
    ('10000000-0000-0000-0000-000000000002', 'STAFF'),
    ('10000000-0000-0000-0000-000000000003', 'ADMIN');