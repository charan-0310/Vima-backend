-- BE-01 Phase 2: Tag endorsements with life event type (marriage, birth, adoption, etc.)
ALTER TABLE cpc.endorsements ADD COLUMN IF NOT EXISTS life_event_type VARCHAR(50);

COMMENT ON COLUMN cpc.endorsements.life_event_type IS 'Life event type for mid-year endorsements: e.g. MARRIAGE, BIRTH, ADOPTION, DIVORCE, etc.';
