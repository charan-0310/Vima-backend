package com.vimainsurance.vimaadmin.service.claim;

/**
 * Helper to store details as valid JSON for the jsonb column.
 * PostgreSQL rejects plain strings (e.g. "Claim submitted...") as invalid JSON; we store {"message":"..."}.
 */
public final class ClaimAuditDetailsJson {
    private ClaimAuditDetailsJson() {}

    /** Serialize a plain message to valid JSON for jsonb: {"message":"..."}. */
    public static String toJsonMessage(String details) {
        if (details == null) return null;
        String escaped = details
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "{\"message\":\"" + escaped + "\"}";
    }

    /** Extract the "message" field from stored JSON, or return the raw string if not in that form. */
    public static String fromJsonMessage(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        if (!stored.trim().startsWith("{\"message\":\"")) return stored;
        int start = stored.indexOf("\"message\":\"") + 11;
        int end = stored.indexOf("\"", start);
        if (end == -1) return stored;
        return stored.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
