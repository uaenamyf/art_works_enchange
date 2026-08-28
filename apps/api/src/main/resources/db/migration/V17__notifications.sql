CREATE TABLE notification_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES notification_events (event_id) ON DELETE RESTRICT,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    link VARCHAR(512),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notifications_user_event UNIQUE (user_id, event_id),
    CONSTRAINT ck_notifications_type CHECK (type IN ('ARTWORK_PUBLISHED', 'ORDER_STATUS_CHANGED', 'PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'DIGITAL_ENTITLEMENT_GRANTED', 'DIGITAL_ASSET_RECALLED'))
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC, id DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;