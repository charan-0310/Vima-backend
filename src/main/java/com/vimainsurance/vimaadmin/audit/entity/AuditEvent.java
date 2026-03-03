package com.vimainsurance.vimaadmin.audit.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.audit.ActionSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_events", schema = "audit")
public class AuditEvent {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "schema_name", length = 63)
    private String schemaName;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_snapshot", columnDefinition = "jsonb")
    private String oldSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_snapshot", columnDefinition = "jsonb")
    private String newSnapshot;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_role", length = 100)
    private String userRole;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "action_source", length = 20)
    @Enumerated(EnumType.STRING)
    private ActionSource actionSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
