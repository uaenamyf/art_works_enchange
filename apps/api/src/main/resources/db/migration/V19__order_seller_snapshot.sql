ALTER TABLE orders
    ADD COLUMN seller_id UUID REFERENCES users (id) ON DELETE RESTRICT;

UPDATE orders o
SET seller_id = a.owner_id
FROM artworks a
WHERE o.artwork_id = a.id
    AND o.seller_id IS NULL;

CREATE INDEX idx_orders_seller_created_at
    ON orders (seller_id, created_at DESC, id DESC);