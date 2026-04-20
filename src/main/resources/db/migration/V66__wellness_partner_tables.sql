-- ============================================================
-- Wellness partners foundation tables
-- Ticket: VIMA-432 (Wellness Partner Integration)
-- ============================================================

-- ============================================================
-- 1) Global partner catalog
-- ============================================================
CREATE TABLE IF NOT EXISTS admin.wellness_partners (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug             VARCHAR(50)  NOT NULL UNIQUE,
    name             VARCHAR(150) NOT NULL,
    description      TEXT,
    long_description TEXT,
    category         VARCHAR(50)  NOT NULL,
    logo_url         VARCHAR(500),
    icon_name        VARCHAR(50),
    card_color       VARCHAR(20)  DEFAULT '#FFFFFF',
    redirect_url     VARCHAR(500),
    redirect_type    VARCHAR(20)  NOT NULL DEFAULT 'DIRECT',
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    metadata         JSONB        DEFAULT '{}'::jsonb,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wellness_partners_category
    ON admin.wellness_partners (category);
CREATE INDEX IF NOT EXISTS idx_wellness_partners_active
    ON admin.wellness_partners (is_active);

-- ============================================================
-- 2) Partner-to-organization mapping
-- ============================================================
CREATE TABLE IF NOT EXISTS admin.wellness_partner_organizations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id          UUID         NOT NULL REFERENCES admin.wellness_partners(id) ON DELETE CASCADE,
    organization_id     UUID         NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    is_active           BOOLEAN      NOT NULL DEFAULT true,
    display_order       INTEGER      NOT NULL DEFAULT 0,
    custom_redirect_url VARCHAR(500),
    config              JSONB        DEFAULT '{}'::jsonb,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (partner_id, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_wpo_org_id
    ON admin.wellness_partner_organizations (organization_id);
CREATE INDEX IF NOT EXISTS idx_wpo_partner_id
    ON admin.wellness_partner_organizations (partner_id);

COMMENT ON COLUMN admin.wellness_partner_organizations.config IS
'Per-org partner configuration JSONB. MantraCare example:
{
  "key_id": 1,
  "invite_code": "VIMA_ABC",
  "auth_type": "JWT",
  "user_identifier_field": "phone"
}';

-- ============================================================
-- 3) Access audit log
-- ============================================================
CREATE TABLE IF NOT EXISTS cpc.wellness_access_logs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID         NOT NULL REFERENCES cpc.organizations(organization_id),
    partner_id       UUID         NOT NULL REFERENCES admin.wellness_partners(id),
    employee_id      UUID         NOT NULL,
    user_identifier  VARCHAR(255) NOT NULL,
    access_type      VARCHAR(20)  NOT NULL, -- LOGIN or REGISTRATION
    status           VARCHAR(20)  NOT NULL, -- SUCCESS or FAILED
    error_message    TEXT,
    accessed_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wellness_access_logs_org
    ON cpc.wellness_access_logs (organization_id);
CREATE INDEX IF NOT EXISTS idx_wellness_access_logs_employee
    ON cpc.wellness_access_logs (employee_id);
CREATE INDEX IF NOT EXISTS idx_wellness_access_logs_accessed_at
    ON cpc.wellness_access_logs (accessed_at);
