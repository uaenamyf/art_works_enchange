CREATE TABLE artworks (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    creator_id UUID NOT NULL REFERENCES creator_profiles (id) ON DELETE RESTRICT,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(10000) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price_amount NUMERIC(19, 2) NOT NULL,
    price_currency CHAR(3) NOT NULL,
    stock INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    review_reason VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_artworks_price_positive CHECK (price_amount > 0),
    CONSTRAINT ck_artworks_stock_non_negative CHECK (stock >= 0),
    CONSTRAINT ck_artworks_status CHECK (
        status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'PUBLISHED', 'SOLD_OUT', 'ARCHIVED', 'BLOCKED')
    )
);

CREATE INDEX idx_artworks_public_listing ON artworks (status, created_at DESC, id DESC);
CREATE INDEX idx_artworks_owner_id ON artworks (owner_id, updated_at DESC, id DESC);
CREATE INDEX idx_artworks_creator_id ON artworks (creator_id, updated_at DESC, id DESC);

CREATE TABLE artwork_media (
    id UUID PRIMARY KEY,
    artwork_id UUID NOT NULL REFERENCES artworks (id) ON DELETE CASCADE,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    alt_text VARCHAR(200),
    sort_order INTEGER NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_artwork_media_object_key UNIQUE (object_key),
    CONSTRAINT uq_artwork_media_sort_order UNIQUE (artwork_id, sort_order),
    CONSTRAINT ck_artwork_media_size_positive CHECK (size_bytes > 0),
    CONSTRAINT ck_artwork_media_sort_order_non_negative CHECK (sort_order >= 0)
);

CREATE INDEX idx_artwork_media_artwork_id ON artwork_media (artwork_id, sort_order);
