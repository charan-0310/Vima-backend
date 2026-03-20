ALTER TABLE cpc.product_catalog
    ADD COLUMN IF NOT EXISTS premium_preview_options jsonb;

COMMENT ON COLUMN cpc.product_catalog.premium_preview_options IS
    'JSON object map: {"sumInsured":"premium"} for TOP_UP/SUPER_TOP_UP preview';

WITH paired AS (
    SELECT
        pc.id AS product_catalog_id,
        si.val AS si_val,
        pr.val AS pr_val
    FROM cpc.product_catalog pc
    JOIN cpc.policies p ON p.policy_id = pc.policy_id
    JOIN LATERAL jsonb_array_elements_text(COALESCE(p.sum_insured_options, '[]'::jsonb)) WITH ORDINALITY AS si(val, ord)
        ON TRUE
    JOIN LATERAL jsonb_array_elements_text(COALESCE(p.topup_premium_options, '[]'::jsonb)) WITH ORDINALITY AS pr(val, ord)
        ON pr.ord = si.ord
    WHERE pc.product_type IN ('TOP_UP', 'SUPER_TOP_UP')
      AND pc.premium_preview_options IS NULL
)
UPDATE cpc.product_catalog pc
SET premium_preview_options = map_obj.obj,
    updated_at = NOW()
FROM (
    SELECT
        product_catalog_id,
        jsonb_object_agg(si_val, to_jsonb((pr_val)::numeric) ORDER BY (si_val)::numeric) AS obj
    FROM paired
    GROUP BY product_catalog_id
) map_obj
WHERE pc.id = map_obj.product_catalog_id;
