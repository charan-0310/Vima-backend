package com.vimainsurance.vimaadmin.audit;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable payload for an audit event (captured in request thread, passed to async writer).
 * Loosely coupled: no dependency on domain entities; JSON snapshots are opaque strings.
 */
@Value
@Builder
@JsonDeserialize(builder = AuditEventPayload.AuditEventPayloadBuilder.class)
public class AuditEventPayload {

    String schemaName;
    String tableName;
    String entityType;
    String entityId;
    String action;
    String oldSnapshot;
    String newSnapshot;
    UUID userId;
    UUID organizationId;
    String userEmail;
    String userRole;
    String correlationId;
    String ipAddress;
    ActionSource actionSource;
    @Builder.Default
    Instant timestamp = Instant.now();
}
