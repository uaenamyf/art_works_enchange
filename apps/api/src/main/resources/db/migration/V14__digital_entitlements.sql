CREATE TABLE digital_entitlements (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE RESTRICT,
    buyer_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    artwork_id UUID NOT NULL REFERENCES artworks (id) ON DELETE RESTRICT,
    asset_version_id UUID NOT NULL REFERENCES digital_asset_versions (id) ON DELETE RESTRICT,
    status VARCHAR(16) NOT NULL,
    granted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_digital_entitlement_order UNIQUE (order_id),
    CONSTRAINT ck_digital_entitlement_status CHECK (status IN ('PENDING', 'AVAILABLE', 'REVOKED'))
);

CREATE INDEX idx_digital_entitlements_buyer_status
    ON digital_entitlements (buyer_id, status, created_at DESC);

CREATE INDEX idx_digital_entitlements_asset_version
    ON digital_entitlements (asset_version_id, status);