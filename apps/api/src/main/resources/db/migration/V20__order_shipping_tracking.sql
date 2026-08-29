ALTER TABLE orders
    ADD COLUMN tracking_number VARCHAR(100),
    ADD COLUMN carrier VARCHAR(80);