package com.vimainsurance.vimaadmin.audit;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.entity.AuditEvent;

import lombok.RequiredArgsConstructor;

/**
 * Performs the transactional persist of an audit event (called from async layer).
 * Separate bean so @Transactional is applied via proxy.
 */
@Component
@RequiredArgsConstructor
public class AuditEventWriter {

    private final AuditEventRepository auditEventRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(AuditEventPayload payload) {
        AuditEvent event = toEntity(payload);
        auditEventRepository.save(event);
    }

    private static AuditEvent toEntity(AuditEventPayload p) {
        return AuditEvent.builder()
                .schemaName(emptyToNull(p.getSchemaName()))
                .tableName(emptyToNull(p.getTableName()))
                .entityType(p.getEntityType())
                .entityId(p.getEntityId())
                .action(p.getAction())
                .oldSnapshot(p.getOldSnapshot())
                .newSnapshot(p.getNewSnapshot())
                .userId(p.getUserId())
                .userEmail(p.getUserEmail())
                .userRole(p.getUserRole())
                .correlationId(p.getCorrelationId())
                .ipAddress(p.getIpAddress())
                .actionSource(p.getActionSource())
                .createdAt(Optional.ofNullable(p.getTimestamp()).orElse(Instant.now()))
                .build();
    }

    private static String emptyToNull(String s) {
        return s != null && !s.isBlank() ? s : null;
    }
}
