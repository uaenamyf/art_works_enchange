CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(2048),
    role VARCHAR(20) NOT NULL,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'CREATOR', 'ADMIN'))
);

CREATE INDEX idx_users_created_at ON users (created_at DESC, id DESC);

CREATE TABLE identity_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_identity_sessions_refresh_token_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_identity_sessions_user_id ON identity_sessions (user_id);
CREATE INDEX idx_identity_sessions_expires_at ON identity_sessions (expires_at);
