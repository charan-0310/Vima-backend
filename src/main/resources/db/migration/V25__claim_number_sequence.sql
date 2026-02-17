-- Sequence table for claim numbers: VIMA-CLM-{YYYY}-{NNNN}
CREATE TABLE IF NOT EXISTS claims.claim_number_sequence (
    claim_year INTEGER NOT NULL PRIMARY KEY,
    next_sequence INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1
);

-- Seed current year so first claim gets 1
INSERT INTO claims.claim_number_sequence (claim_year, next_sequence)
SELECT EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1
WHERE NOT EXISTS (SELECT 1 FROM claims.claim_number_sequence LIMIT 1);
