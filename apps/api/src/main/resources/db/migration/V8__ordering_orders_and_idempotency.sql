CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE,
    buyer_id UUID NOT NULL REFERENCES users (id),
    artwork_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    shipping_address JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_orders_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT ck_orders_amount CHECK (unit_price > 0 AND subtotal > 0),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'REFUNDING', 'REFUNDED'))
);

CREATE INDEX idx_orders_buyer_created_at ON orders (buyer_id, created_at DESC, id DESC);

CREATE TABLE order_idempotency_keys (
    buyer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    operation VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_order_idempotency_keys PRIMARY KEY (buyer_id, operation, idempotency_key)
);