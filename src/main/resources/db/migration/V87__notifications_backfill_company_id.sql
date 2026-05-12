-- Backfill admin.notifications.company_id where it was never set (HR inbox requires company_id or explicit null+recipient match).
-- Dedup key formats: EVENT:entityUuid:... (see FlagshipNotificationService / ClaimsNotificationEmitterService).

-- Endorsement completed: ENDORSEMENT_COMPLETED:{endorsementId}:{recipientAdminUserId}
UPDATE admin.notifications n
SET company_id = e.organization_id,
    updated_at = NOW()
FROM cpc.endorsements e
WHERE n.company_id IS NULL
  AND n.event_type = 'ENDORSEMENT_COMPLETED'
  AND split_part(n.dedup_key, ':', 2) = e.endorsement_id::text;

-- Claim events: {EVENT}:{claimId}:{recipientId}
UPDATE admin.notifications n
SET company_id = c.organization_id,
    updated_at = NOW()
FROM claims.claims c
WHERE n.company_id IS NULL
  AND n.event_type IN (
    'EMPLOYEE_CLAIM_SUBMITTED',
    'EMPLOYEE_CLAIM_APPROVED',
    'EMPLOYEE_CLAIM_REJECTED',
    'EMPLOYEE_CLAIM_SETTLED'
  )
  AND split_part(n.dedup_key, ':', 2) = c.id::text;

-- Claim query events: {EVENT}:{claimId}:{queryId}:{recipientId}
UPDATE admin.notifications n
SET company_id = c.organization_id,
    updated_at = NOW()
FROM claims.claims c
WHERE n.company_id IS NULL
  AND n.event_type IN (
    'EMPLOYEE_CLAIM_QUERY_RAISED',
    'EMPLOYEE_CLAIM_QUERY_RESPONDED',
    'EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED'
  )
  AND split_part(n.dedup_key, ':', 2) = c.id::text;

-- Enrollment (window id in second segment)
UPDATE admin.notifications n
SET company_id = w.organization_id,
    updated_at = NOW()
FROM cpc.enrollment_windows w
WHERE n.company_id IS NULL
  AND n.event_type IN (
    'ENROLLMENT_ALL_SUBMITTED',
    'ENROLLMENT_SUBMISSION_APPROVED',
    'ENROLLMENT_WINDOW_CLOSED'
  )
  AND split_part(n.dedup_key, ':', 2) = w.id::text;

-- Enrollment window opened / closing soon: {EVENT}:{windowId} (no recipient suffix in dedup key)
UPDATE admin.notifications n
SET company_id = w.organization_id,
    updated_at = NOW()
FROM cpc.enrollment_windows w
WHERE n.company_id IS NULL
  AND n.event_type IN ('ENROLLMENT_WINDOW_OPENED', 'ENROLLMENT_WINDOW_CLOSING_SOON')
  AND split_part(n.dedup_key, ':', 2) = w.id::text;
