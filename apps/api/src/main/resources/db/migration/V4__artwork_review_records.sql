CREATE TABLE artwork_review_records (
    id UUID PRIMARY KEY,
    artwork_id UUID NOT NULL REFERENCES artworks (id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    decision VARCHAR(20) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_artwork_review_decision CHECK (decision IN ('APPROVED', 'REJECTED'))
);

CREATE INDEX idx_artwork_review_records_artwork_id
    ON artwork_review_records (artwork_id, created_at DESC, id DESC);
