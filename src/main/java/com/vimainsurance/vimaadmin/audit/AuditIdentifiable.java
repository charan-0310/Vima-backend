package com.vimainsurance.vimaadmin.audit;

import java.util.UUID;

/**
 * Optional interface for audited return types. If a method annotated with
 * {@link AuditedOperation} returns an object implementing this interface, the
 * aspect uses {@link #getAuditEntityId()} for the audit event's entity_id and
 * {@link #getAuditOrganizationId()} for organization context when non-null.
 */
public interface AuditIdentifiable {

    /**
     * Returns the identifier for this entity in audit logs (e.g. UUID string, primary key).
     */
    String getAuditEntityId();

    /**
     * Optional: organization ID for tenant-scoped audit. Default null; override when entity is org-scoped.
     */
    default UUID getAuditOrganizationId() {
        return null;
    }
}
