CREATE TABLE creator_follows (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    creator_id UUID NOT NULL REFERENCES creator_profiles (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_creator_follows PRIMARY KEY (user_id, creator_id)
);

CREATE INDEX idx_creator_follows_creator ON creator_follows (creator_id, created_at DESC, user_id DESC);
