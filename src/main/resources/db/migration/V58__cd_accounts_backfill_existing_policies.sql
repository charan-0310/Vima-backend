-- Backfill missing CD accounts for existing policies where insurer_name is null/blank.
-- Uses provider name fallback from admin.insurance_providers via insurance_provider_id.

-- 1) Create default CD accounts for policies still missing cd_account_id.
INSERT INTO cpc.cd_accounts (organization_id, insurer_name, label, cd_balance, status)
SELECT
    p.organization_id,
    COALESCE(NULLIF(btrim(p.insurer_name), ''), ip.provider_name) AS resolved_insurer_name,
    NULL AS label,
    0 AS cd_balance,
    'ACTIVE' AS status
FROM cpc.policies p
JOIN cpc.organizations o
    ON o.organization_id = p.organization_id
LEFT JOIN admin.insurance_providers ip
    ON ip.provider_id = p.insurance_provider_id
WHERE p.cd_account_id IS NULL
  AND p.organization_id IS NOT NULL
  AND COALESCE(NULLIF(btrim(p.insurer_name), ''), ip.provider_name) IS NOT NULL
GROUP BY p.organization_id, COALESCE(NULLIF(btrim(p.insurer_name), ''), ip.provider_name)
ON CONFLICT (organization_id, insurer_name) WHERE label IS NULL
DO NOTHING;

-- 2) Link policies to resolved default accounts.
WITH policy_resolved AS (
    SELECT
        p.policy_id,
        p.organization_id,
        COALESCE(NULLIF(btrim(p.insurer_name), ''), ip.provider_name) AS resolved_insurer_name
    FROM cpc.policies p
    LEFT JOIN admin.insurance_providers ip
        ON ip.provider_id = p.insurance_provider_id
    WHERE p.cd_account_id IS NULL
      AND p.organization_id IS NOT NULL
      AND EXISTS (
          SELECT 1
          FROM cpc.organizations o
          WHERE o.organization_id = p.organization_id
      )
      AND COALESCE(NULLIF(btrim(p.insurer_name), ''), ip.provider_name) IS NOT NULL
)
UPDATE cpc.policies p
SET cd_account_id = ca.cd_account_id
FROM policy_resolved pr
JOIN cpc.cd_accounts ca
    ON ca.organization_id = pr.organization_id
   AND ca.insurer_name = pr.resolved_insurer_name
   AND ca.label IS NULL
WHERE p.policy_id = pr.policy_id
  AND p.cd_account_id IS NULL;

-- 3) Backfill unresolved transactions using newly linked policy accounts.
UPDATE cpc.cd_balance_transactions tx
SET cd_account_id = p.cd_account_id
FROM cpc.policies p
WHERE tx.cd_account_id IS NULL
  AND tx.policy_id = p.policy_id
  AND p.cd_account_id IS NOT NULL;
