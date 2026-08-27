CREATE TABLE favorite_collections (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    artwork_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_favorite_collections PRIMARY KEY (user_id, artwork_id)
);

CREATE INDEX idx_favorite_collections_artwork ON favorite_collections (artwork_id, created_at DESC);