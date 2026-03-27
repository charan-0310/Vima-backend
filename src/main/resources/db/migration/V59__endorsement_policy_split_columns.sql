ALTER TABLE cpc.endorsements
    ADD COLUMN IF NOT EXISTS policy_id BIGINT,
    ADD COLUMN IF NOT EXISTS parent_endorsement_id UUID,
    ADD COLUMN IF NOT EXISTS split_group_id UUID;

ALTER TABLE cpc.endorsements
    ADD CONSTRAINT fk_endorsements_policy
        FOREIGN KEY (policy_id) REFERENCES cpc.policies (policy_id);

ALTER TABLE cpc.endorsements
    ADD CONSTRAINT fk_endorsements_parent
        FOREIGN KEY (parent_endorsement_id) REFERENCES cpc.endorsements (endorsement_id);

CREATE INDEX IF NOT EXISTS idx_endorsements_policy_id ON cpc.endorsements (policy_id);
CREATE INDEX IF NOT EXISTS idx_endorsements_split_group_id ON cpc.endorsements (split_group_id);
