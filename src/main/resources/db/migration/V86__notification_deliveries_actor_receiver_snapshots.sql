ALTER TABLE admin.notification_deliveries
    ADD COLUMN IF NOT EXISTS receiver_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS receiver_role VARCHAR(80),
    ADD COLUMN IF NOT EXISTS creator_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS creator_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS creator_role VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_receiver_email
    ON admin.notification_deliveries (receiver_email);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_creator_email
    ON admin.notification_deliveries (creator_email);
