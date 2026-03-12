package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollment_submissions", schema = "cpc")
public class EnrollmentSubmission {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, referencedColumnName = "individual_id")
    private Deals employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_window_id", nullable = false)
    private EnrollmentWindows enrollmentWindow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id")
    private EnrollmentInvitation invitation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endorsement_id", referencedColumnName = "endorsement_id")
    private Endorsement endorsement;

    @Column(name = "reference_number", length = 50, unique = true)
    private String referenceNumber;

    /**
     * Backed by Postgres named enum: cpc.endorsement_status_enum
     * Values: draft, submitted, approved, rejected, endorsed
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private EnrollementStatus status = EnrollementStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plan_selections", columnDefinition = "jsonb")
    private String planSelections = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nominee_data", columnDefinition = "jsonb")
    private String nomineeData = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personal_details", columnDefinition = "jsonb")
    private String personalDetails = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dependents", columnDefinition = "jsonb")
    private String dependents = "{}";

    @Column(name = "stage", length = 50)
    private String stage = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "premium_breakdown", columnDefinition = "jsonb")
    private String premiumBreakdown = "{}";

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    private AdminUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "declaration_accepted")
    private Boolean declarationAccepted = false;

    @Column(name = "declaration_timestamp")
    private LocalDateTime declarationTimestamp;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "declaration_ip_address", columnDefinition = "inet")
    private String declarationIpAddress;

    @Version
    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "deduction_frequency", length = 20)
    private String deductionFrequency;

    @Column(name = "total_employee_annual_premium", precision = 15, scale = 2)
    private BigDecimal totalEmployeeAnnualPremium;

    @Column(name = "total_employer_annual_premium", precision = 15, scale = 2)
    private BigDecimal totalEmployerAnnualPremium;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cost_sharing_snapshot", columnDefinition = "jsonb")
    private String costSharingSnapshot = "{}";

    @Column(name = "deduction_amount_per_period", precision = 15, scale = 2)
    private BigDecimal deductionAmountPerPeriod;

    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;

    @Column(name = "consent_text_snapshot", columnDefinition = "TEXT")
    private String consentTextSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

