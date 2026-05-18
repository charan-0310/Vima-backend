-- F-18: Audit retention metadata and monthly partition helper (ops applies parent conversion first)

COMMENT ON TABLE audit.audit_events IS
    'Application audit trail (F-18). Hot retention: 12 months in RDS. Archive partitions >12mo to S3 Glacier (8y). '
    'Login events: Keycloak Events → S3. See docs/prd/audit/F-18_Audit_Retention_and_Access.md';

CREATE TABLE IF NOT EXISTS audit.retention_archive_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partition_month DATE NOT NULL,
    s3_bucket       VARCHAR(255),
    s3_object_key   VARCHAR(512),
    row_count       BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    error_message   TEXT,
    archived_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_retention_archive_runs_month
    ON audit.retention_archive_runs (partition_month);

COMMENT ON TABLE audit.retention_archive_runs IS
    'Tracks monthly export of audit.audit_events to S3 for long-term retention (F-18)';

-- Creates audit.audit_events_YYYY_MM only when parent is RANGE-partitioned on created_at.
CREATE OR REPLACE FUNCTION audit.ensure_monthly_partition(p_month DATE)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    part_name   TEXT;
    start_ts    TIMESTAMPTZ;
    end_ts      TIMESTAMPTZ;
    parent_reg  REGCLASS;
    is_partitioned BOOLEAN;
BEGIN
    IF p_month IS NULL THEN
        RAISE EXCEPTION 'p_month is required';
    END IF;

    part_name := 'audit_events_' || to_char(p_month, 'YYYY_MM');
    start_ts := date_trunc('month', p_month::TIMESTAMPTZ);
    end_ts := start_ts + INTERVAL '1 month';

    SELECT c.relkind = 'p'
    INTO is_partitioned
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'audit' AND c.relname = 'audit_events';

    IF NOT COALESCE(is_partitioned, FALSE) THEN
        RAISE NOTICE 'audit.audit_events is not partitioned; skip creating %', part_name;
        RETURN;
    END IF;

    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS audit.%I PARTITION OF audit.audit_events FOR VALUES FROM (%L) TO (%L)',
        part_name, start_ts, end_ts
    );
END;
$$;

COMMENT ON FUNCTION audit.ensure_monthly_partition(DATE) IS
    'Creates monthly child partition for audit.audit_events after DBA converts parent to PARTITION BY RANGE (created_at)';
