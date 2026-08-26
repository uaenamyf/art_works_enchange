CREATE TABLE media_objects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    upload_status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    uploaded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_media_objects_object_key UNIQUE (object_key),
    CONSTRAINT ck_media_objects_size_positive CHECK (size_bytes > 0),
    CONSTRAINT ck_media_objects_upload_status CHECK (upload_status IN ('PENDING', 'READY', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_media_objects_owner_status ON media_objects (owner_id, upload_status, created_at DESC);
