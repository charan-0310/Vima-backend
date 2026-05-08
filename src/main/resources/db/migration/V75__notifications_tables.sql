-- Unified notifications (VIMA-465): per-recipient rows + channel delivery audit
CREATE TABLE admin.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    company_id UUID REFERENCES cpc.organizations(organization_id) ON DELETE SET NULL,
    recipient_admin_user_id UUID NOT NULL REFERENCES admin.admin_users(id) ON DELETE CASCADE,
    event_type VARCHAR(80) NOT NULL,
    category VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    title VARCHAR(500) NOT NULL,
    body TEXT,
    email_subject VARCHAR(500),
    email_template VARCHAR(200),
    email_template_vars JSONB,
    deep_link_url VARCHAR(2000),
    dedup_key VARCHAR(512) NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_notifications_dedup_key UNIQUE (dedup_key)
);

CREATE INDEX idx_notifications_recipient_read ON admin.notifications (recipient_admin_user_id, read_at);
CREATE INDEX idx_notifications_recipient_created ON admin.notifications (recipient_admin_user_id, created_at DESC);
CREATE INDEX idx_notifications_company ON admin.notifications (company_id);

CREATE TABLE admin.notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES admin.notifications(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    external_message_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_deliveries_status_retry
    ON admin.notification_deliveries (status, next_retry_at);
CREATE INDEX idx_notification_deliveries_notification
    ON admin.notification_deliveries (notification_id);

COMMENT ON TABLE admin.notifications IS 'In-app + cross-channel notification root; one row per recipient per logical event (dedup_key)';
COMMENT ON TABLE admin.notification_deliveries IS 'Per-channel send attempts and retry state for a notification';
