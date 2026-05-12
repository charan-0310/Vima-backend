package com.vimainsurance.vimaadmin.audit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes to {@code audit.audit_events} for flows that cannot use {@link AuditedOperation}
 * (no JWT yet, {@link org.springframework.http.ResponseEntity} return shape, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAuditPublisher {

    private static final int MAX_SNAPSHOT_CHARS = 50_000;

    private final IAuditService auditService;
    private final ObjectMapper objectMapper;
    private final JwtUserExtractor jwtUserExtractor;

    /**
     * Publishes one row to {@code audit.audit_events}. When override fields are null, actor is taken from
     * {@link AuditContextSupplier} and {@link JwtUserExtractor} (JWT / employee token).
     */
    public void publish(
            String schemaName,
            String tableName,
            String entityType,
            String action,
            String entityId,
            UUID organizationId,
            UUID actorUserIdOverride,
            String actorEmailOverride,
            String actorRoleOverride,
            String oldSnapshot,
            Object newSnapshot) {

        UUID userId = actorUserIdOverride;
        if (userId == null) {
            userId = AuditContextSupplier.getCurrentUserId().orElse(null);
        }
        if (userId == null) {
            userId = jwtUserExtractor.getCurrentUserId();
        }
        if (userId == null) {
            userId = jwtUserExtractor.getCurrentEmployeeId();
        }

        String email = actorEmailOverride;
        if (email == null || email.isBlank()) {
            email = AuditContextSupplier.getUserEmail();
        }
        if (email == null || email.isBlank()) {
            email = jwtUserExtractor.getCurrentEmail();
        }

        String role = actorRoleOverride;
        if (role == null || role.isBlank()) {
            role = AuditContextSupplier.getUserRole();
        }
        if (role == null || role.isBlank()) {
            var ur = jwtUserExtractor.getCurrentUserRole();
            if (ur != null) {
                role = ur.getValue();
            } else if (jwtUserExtractor.getCurrentEmployeeId() != null) {
                role = "EMPLOYEE";
            }
        }

        auditService.writeAsync(AuditEventPayload.builder()
                .schemaName(blankToNull(schemaName))
                .tableName(blankToNull(tableName))
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldSnapshot(truncate(oldSnapshot))
                .newSnapshot(truncate(toJson(newSnapshot)))
                .userId(userId)
                .organizationId(organizationId)
                .userEmail(blankToNull(email))
                .userRole(blankToNull(role))
                .correlationId(AuditContextSupplier.getCorrelationId())
                .ipAddress(AuditContextSupplier.getIpAddress())
                .actionSource(AuditContextSupplier.getActionSource())
                .build());
    }

    /** Convenience when actor is the authenticated admin/employee from JWT / thread context. */
    public void publishAuthenticated(
            String schemaName,
            String tableName,
            String entityType,
            String action,
            String entityId,
            UUID organizationId,
            Object newSnapshot) {
        publish(schemaName, tableName, entityType, action, entityId, organizationId,
                null, null, null, null, newSnapshot);
    }

    private static String blankToNull(String s) {
        return s != null && !s.isBlank() ? s : null;
    }

    private static String truncate(String json) {
        if (json == null || json.length() <= MAX_SNAPSHOT_CHARS) {
            return json;
        }
        return json.substring(0, MAX_SNAPSHOT_CHARS) + " [truncated, total " + json.length() + " chars]";
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.trace("[audit] snapshot serialization failed: {}", e.getMessage());
            return null;
        }
    }
}
