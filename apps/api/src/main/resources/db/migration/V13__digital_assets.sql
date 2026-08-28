CREATE TABLE digital_assets (
    id UUID PRIMARY KEY,
    artwork_id UUID NOT NULL REFERENCES artworks (id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    current_version INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_digital_asset_status CHECK (status IN ('DRAFT', 'READY', 'RECALLED', 'BLOCKED'))
);

CREATE UNIQUE INDEX uq_digital_assets_artwork_id ON digital_assets (artwork_id);

CREATE TABLE digital_asset_versions (
    id UUID PRIMARY KEY,
    digital_asset_id UUID NOT NULL REFERENCES digital_assets (id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    scan_status VARCHAR(20) NOT NULL,
    scan_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    scanned_at TIMESTAMPTZ,
    CONSTRAINT uq_digital_asset_version UNIQUE (digital_asset_id, version),
    CONSTRAINT uq_digital_asset_object_key UNIQUE (object_key),
    CONSTRAINT ck_digital_asset_version_positive CHECK (version >= 1),
    CONSTRAINT ck_digital_asset_size_positive CHECK (size_bytes > 0),
    CONSTRAINT ck_digital_asset_scan_status CHECK (scan_status IN ('PENDING_SCAN', 'SCANNING', 'READY', 'QUARANTINED')),
    CONSTRAINT ck_digital_asset_sha256 CHECK (sha256 ~ '^[0-9a-fA-F]{64}$')
);

CREATE INDEX idx_digital_asset_versions_scan_queue
    ON digital_asset_versions (scan_status, created_at, id)
    WHERE scan_status IN ('PENDING_SCAN', 'SCANNING');