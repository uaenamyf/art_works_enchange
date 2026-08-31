ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type CHECK (
        type IN (
            'CREATOR_FOLLOWED',
            'ARTWORK_PUBLISHED',
            'ORDER_CREATED',
            'ORDER_STATUS_CHANGED',
            'PAYMENT_SUCCEEDED',
            'PAYMENT_FAILED',
            'DIGITAL_ENTITLEMENT_GRANTED',
            'DIGITAL_ASSET_RECALLED',
            'ARTWORK_REVIEW_APPROVED',
            'ARTWORK_REVIEW_REJECTED'
        )
    );