CREATE TABLE creator_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    slug VARCHAR(40) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    bio VARCHAR(2000) NOT NULL,
    location VARCHAR(100),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url VARCHAR(2048),
    cover_url VARCHAR(2048),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_creator_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uq_creator_profiles_slug UNIQUE (slug)
);

CREATE INDEX idx_creator_profiles_created_at ON creator_profiles (created_at DESC, id DESC);
