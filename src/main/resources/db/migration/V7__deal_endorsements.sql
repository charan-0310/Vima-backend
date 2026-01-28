-- Create join table for many-to-many relationship between Deals (customers) and Endorsements

CREATE TABLE IF NOT EXISTS cpc.deal_endorsements (
    deal_endorsement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    individual_id UUID NOT NULL REFERENCES cpc.customers(individual_id) ON DELETE CASCADE,
    endorsement_id UUID NOT NULL REFERENCES cpc.endorsements(endorsement_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(individual_id, endorsement_id) -- Prevents duplicate entries for the same deal/endorsement pair
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_deal_endorsements_individual_id ON cpc.deal_endorsements(individual_id);
CREATE INDEX IF NOT EXISTS idx_deal_endorsements_endorsement_id ON cpc.deal_endorsements(endorsement_id);

-- Create a trigger to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_deal_endorsements_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_deal_endorsements_updated_at
    BEFORE UPDATE ON cpc.deal_endorsements
    FOR EACH ROW
    EXECUTE FUNCTION update_deal_endorsements_updated_at();

