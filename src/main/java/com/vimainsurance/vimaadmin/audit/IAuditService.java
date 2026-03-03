package com.vimainsurance.vimaadmin.audit;

/**
 * Audit service interface (loosely coupled: callers depend only on this contract).
 * Implementations write asynchronously to the audit store.
 */
public interface IAuditService {

    /**
     * Enqueue an audit event for async write. Does not block.
     * Context must be captured in the calling thread before invocation.
     */
    void writeAsync(AuditEventPayload payload);
}
