CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    trace_id UUID,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    CONSTRAINT ck_outbox_event_version_positive CHECK (event_version >= 1),
    CONSTRAINT ck_outbox_aggregate_version_non_negative CHECK (aggregate_version >= 0),
    CONSTRAINT ck_outbox_attempts_non_negative CHECK (attempts >= 0),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD'))
);

CREATE INDEX idx_outbox_pending_delivery
    ON outbox_events (next_attempt_at, occurred_at, event_id)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id, aggregate_version);