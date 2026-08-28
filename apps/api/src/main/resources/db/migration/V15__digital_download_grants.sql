CREATE TABLE digital_download_grants (
    id UUID PRIMARY KEY,
    entitlement_id UUID NOT NULL REFERENCES digital_entitlements (id) ON DELETE RESTRICT,
    token_digest CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    request_ip_hash CHAR(64),
    user_agent_hash CHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_digital_download_grant_digest UNIQUE (token_digest),
    CONSTRAINT ck_digital_download_grant_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_digital_download_grants_entitlement
    ON digital_download_grants (entitlement_id, created_at DESC);

CREATE INDEX idx_digital_download_grants_expiry
    ON digital_download_grants (expires_at)
    WHERE used_at IS NULL AND revoked_at IS NULL;