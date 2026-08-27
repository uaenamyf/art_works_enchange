CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    artwork_id UUID NOT NULL,
    movement_type VARCHAR(16) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_inventory_reservation_type UNIQUE (reservation_id, movement_type),
    CONSTRAINT ck_inventory_movement_type CHECK (movement_type IN ('RESERVE', 'RELEASE')),
    CONSTRAINT ck_inventory_movement_quantity CHECK (quantity BETWEEN 1 AND 99)
);

CREATE INDEX idx_inventory_movements_artwork_created_at
    ON inventory_movements (artwork_id, created_at DESC, id DESC);