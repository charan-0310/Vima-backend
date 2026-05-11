-- MantraCare HTTP defaults (key id stays in application properties with PEM)

UPDATE admin.wellness_partners
SET metadata = COALESCE(metadata, '{}'::jsonb) || jsonb_build_object(
    'api_base_url', 'https://api.mantracare.com',
    'set_user_path', '/partner/user',
    'token_validity_seconds', 300,
    'connect_timeout_ms', 3000,
    'read_timeout_ms', 8000
),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'mantracare';
