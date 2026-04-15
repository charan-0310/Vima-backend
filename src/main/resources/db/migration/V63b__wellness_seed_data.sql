-- ============================================================
-- Wellness partners seed data
-- Ticket: VIMA-432
-- NOTE: V57b is already consumed by existing repository versions;
-- using V63b to avoid Flyway version conflict.
-- ============================================================

INSERT INTO admin.wellness_partners (
    slug,
    name,
    description,
    category,
    redirect_url,
    redirect_type,
    icon_name,
    card_color,
    metadata
)
VALUES
    (
        'mantracare',
        'MantraCare',
        'Access telemedicine consultations and mental wellness support from certified professionals.',
        'TELEMEDICINE',
        NULL,
        'BACKEND_TOKEN',
        'stethoscope',
        '#4F46E5',
        '{"features": ["Video Consultations", "Mental Health", "Therapy Sessions"]}'::jsonb
    ),
    (
        'redcliff',
        'RedCliff Labs',
        'Book at-home blood tests and health checkups with trusted diagnostic partners.',
        'DIAGNOSTICS',
        'https://www.redcliffelabs.com',
        'DIRECT',
        'test-tubes',
        '#DC2626',
        '{"features": ["At-Home Tests", "Full Body Checkup", "Specialized Panels"]}'::jsonb
    )
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    redirect_url = EXCLUDED.redirect_url,
    redirect_type = EXCLUDED.redirect_type,
    icon_name = EXCLUDED.icon_name,
    card_color = EXCLUDED.card_color,
    metadata = EXCLUDED.metadata,
    is_active = true,
    updated_at = CURRENT_TIMESTAMP;
