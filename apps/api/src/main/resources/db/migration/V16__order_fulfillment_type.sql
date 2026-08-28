ALTER TABLE orders
    ADD COLUMN fulfillment_type VARCHAR(16) NOT NULL DEFAULT 'PHYSICAL';

ALTER TABLE orders
    ALTER COLUMN shipping_address DROP NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_fulfillment_type CHECK (fulfillment_type IN ('PHYSICAL', 'DIGITAL'));

ALTER TABLE orders
    ADD CONSTRAINT ck_orders_shipping_address_by_fulfillment CHECK (
        (fulfillment_type = 'PHYSICAL' AND shipping_address IS NOT NULL)
        OR (fulfillment_type = 'DIGITAL' AND shipping_address IS NULL)
    );