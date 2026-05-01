ALTER TABLE notification_messages
    DROP CONSTRAINT IF EXISTS ck_notification_messages_type;

ALTER TABLE notification_messages
    ADD CONSTRAINT ck_notification_messages_type
        CHECK (notification_type IN (
            'ORDER_CREATED',
            'ORDER_CONFIRMED',
            'ORDER_CANCELLED',
            'STOCK_RESERVED',
            'STOCK_RELEASED',
            'STOCK_CONSUMED',
            'LOW_STOCK_ALERT'
        ));
