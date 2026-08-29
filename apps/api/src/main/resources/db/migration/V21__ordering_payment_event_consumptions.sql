CREATE TABLE ordering_payment_event_consumptions (
    event_id UUID NOT NULL,
    order_id UUID NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, order_id)
);

CREATE UNIQUE INDEX uq_ordering_payment_event_consumptions_event
    ON ordering_payment_event_consumptions (event_id);