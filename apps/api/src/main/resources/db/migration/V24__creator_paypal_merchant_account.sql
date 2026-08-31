ALTER TABLE creator_profiles
    ADD COLUMN paypal_merchant_id VARCHAR(64);

ALTER TABLE creator_profiles
    ADD CONSTRAINT ck_creator_paypal_merchant_id
    CHECK (paypal_merchant_id IS NULL OR paypal_merchant_id ~ '^[A-Za-z0-9_-]+$');