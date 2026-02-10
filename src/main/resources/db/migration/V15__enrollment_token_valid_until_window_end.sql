-- Support "token valid until window end" and "reminder sends same link":
-- New invitations use a deterministic token (derived from invitation id) so the same link
-- can be sent in reminders. Old invitations (token_deterministic = false/null) keep
-- current behavior: reminder generates a new token.
ALTER TABLE cpc.enrollment_invitations
  ADD COLUMN IF NOT EXISTS token_deterministic BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN cpc.enrollment_invitations.token_deterministic IS
  'When true, token is derived from invitation id; reminder emails resend the same link. When false, reminder generates a new token.';
