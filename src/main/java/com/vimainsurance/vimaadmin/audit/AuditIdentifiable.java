package com.vimainsurance.vimaadmin.audit;

/**
 * Optional interface for audited return types. If a method annotated with
 * {@link AuditedOperation} returns an object implementing this interface, the
 * aspect uses {@link #getAuditEntityId()} for the audit event's entity_id.
 * Otherwise, the aspect falls back to convention-based lookup (getter ending with "Id").
 */
public interface AuditIdentifiable {

    /**
     * Returns the identifier for this entity in audit logs (e.g. UUID string, primary key).
     */
    String getAuditEntityId();
}
