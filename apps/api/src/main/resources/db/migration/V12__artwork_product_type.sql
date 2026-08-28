ALTER TABLE artworks
    ADD COLUMN product_type VARCHAR(16) NOT NULL DEFAULT 'PHYSICAL';

ALTER TABLE artworks
    ADD CONSTRAINT ck_artworks_product_type CHECK (product_type IN ('PHYSICAL', 'DIGITAL'));