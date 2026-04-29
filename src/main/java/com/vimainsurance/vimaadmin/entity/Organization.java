package com.vimainsurance.vimaadmin.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.Industry;

import com.vimainsurance.vimaadmin.audit.AuditIdentifiable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "organizations", schema = "cpc")
public class Organization implements AuditIdentifiable {

    @Id
    @GeneratedValue
    @Column(name = "organization_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID organizationId;

    @Override
    public String getAuditEntityId() {
        return organizationId != null ? organizationId.toString() : null;
    }

    @Override
    public UUID getAuditOrganizationId() {
        return organizationId;
    }

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "organization_displayname", nullable = false)
    private String organizationDisplayName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "primary_contact_name", length = 20)
    private String primaryContactName;

    @Column(name = "primary_contact_email")
    private String primaryContactEmail;

    @Column(name = "primary_contact_phone", length = 20)
    private String primaryContactPhone;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_demo_org", nullable = false)
    private Boolean isDemoOrg = false;

    @Column(name = "demo_created_at")
    private LocalDateTime demoCreatedAt;

    @Column(name = "demo_seed_completed_at")
    private LocalDateTime demoSeedCompletedAt;

    @Column(name = "demo_last_assigned_email")
    private String demoLastAssignedEmail;

    @Column(name = "demo_expires_at")
    private LocalDateTime demoExpiresAt;

    @Column(name = "registered_address", length = 250, nullable = false)
    private String registeredAddress;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "industry")
    private Industry industry;
}
