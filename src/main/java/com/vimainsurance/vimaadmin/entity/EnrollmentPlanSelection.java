package com.vimainsurance.vimaadmin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enrollment_plan_selections", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentPlanSelection {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_submission_id", nullable = false)
    private EnrollmentSubmission enrollmentSubmission;

    @Column(name = "plan_type", nullable = false, length = 50)
    private String planType;

    @Column(name = "opted", nullable = false)
    @Builder.Default
    private Boolean opted = true;

    @Column(name = "coverage_amount", precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    @Column(name = "premium", precision = 15, scale = 2)
    private BigDecimal premium;

    @Column(name = "sum_insured", precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "deductible_amount", precision = 15, scale = 2)
    private BigDecimal deductibleAmount;

    @Column(name = "topup_plan_option_id")
    private UUID topupPlanOptionId;

    @Column(name = "is_voluntary")
    @Builder.Default
    private Boolean isVoluntary = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
