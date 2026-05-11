ALTER TABLE admin.notifications
    ADD COLUMN IF NOT EXISTS is_starred BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_starred_created
    ON admin.notifications (recipient_admin_user_id, is_starred, created_at DESC);
