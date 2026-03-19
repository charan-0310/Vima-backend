package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_policy_map", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePolicyMap {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "individual_id", nullable = false)
    private UUID individualId;

    @Column(name = "primary_employee_id")
    private UUID primaryEmployeeId;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "sum_insured", precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "coverage_tier", length = 50)
    private String coverageTier;

    @Column(name = "is_voluntary", nullable = false)
    @Builder.Default
    private Boolean isVoluntary = false;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "enrollment_window_id")
    private UUID enrollmentWindowId;

    @Column(name = "endorsement_id")
    private UUID endorsementId;

    @Column(name = "enrollment_submission_id")
    private UUID enrollmentSubmissionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
