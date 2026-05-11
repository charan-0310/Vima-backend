ALTER TABLE admin.notifications
    ADD COLUMN IF NOT EXISTS receiver_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS receiver_role VARCHAR(80),
    ADD COLUMN IF NOT EXISTS creator_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS creator_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS creator_role VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_notifications_receiver_email
    ON admin.notifications (receiver_email);

CREATE INDEX IF NOT EXISTS idx_notifications_creator_email
    ON admin.notifications (creator_email);
