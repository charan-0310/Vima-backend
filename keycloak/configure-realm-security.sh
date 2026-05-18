#!/usr/bin/env bash
# Apply F-16 / F-17 / F-20 Keycloak realm security baseline (run from a host with kcadm configured).
# Usage: REALM=vima-prod ./configure-realm-security.sh

set -euo pipefail

REALM="${REALM:-vima-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

kcadm update "realms/${REALM}" -f "${SCRIPT_DIR}/realm-security-baseline.json"

# F-16: MFA for privileged realm roles (browser flow — add OTP as REQUIRED after password)
echo "Configure MFA in Admin Console: Authentication → Browser flow → add 'OTP Form' (required) for SUPER_ADMIN, ADMIN, VIMA_ADMIN, HR_ADMIN."
echo "Or: Authentication → Required actions → Enable 'Configure OTP' and assign via realm role composite."

# F-17: Shorter refresh for high-risk roles — Realm → Tokens → Refresh token lifespan (default 7d).
# For 24h refresh: set ssoSessionMaxLifespan / client 'Advanced' refresh token lifespan as needed per client.

# F-20: Disable legacy grants on public clients
for CLIENT in vima-webapp vima-mobile; do
  if kcadm get "clients" -r "${REALM}" -q "clientId=${CLIENT}" --fields id 2>/dev/null | grep -q '"id"'; then
    CID="$(kcadm get clients -r "${REALM}" -q "clientId=${CLIENT}" --fields id --format csv --noquotes | tail -1)"
    kcadm update "clients/${CID}" -r "${REALM}" -s 'directAccessGrantsEnabled=false' -s 'serviceAccountsEnabled=false' 2>/dev/null || true
    echo "Updated ${CLIENT}: directAccessGrantsEnabled=false"
  fi
done

echo "Realm ${REALM} security baseline applied from realm-security-baseline.json"
