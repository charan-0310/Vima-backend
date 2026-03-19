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
@Table(name = "payroll_deduction_schedules", schema = "cpc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollDeductionSchedule {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "enrollment_submission_id")
    private UUID enrollmentSubmissionId;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(name = "plan_type", length = 50)
    private String planType;

    @Column(name = "coverage_amount", precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    @Column(name = "total_premium_annual", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPremiumAnnual;

    @Column(name = "employer_share_annual", nullable = false, precision = 15, scale = 2)
    private BigDecimal employerShareAnnual;

    @Column(name = "employee_share_annual", nullable = false, precision = 15, scale = 2)
    private BigDecimal employeeShareAnnual;

    @Column(name = "deduction_frequency", nullable = false, length = 20)
    private String deductionFrequency;

    @Column(name = "deduction_amount_per_period", nullable = false, precision = 15, scale = 2)
    private BigDecimal deductionAmountPerPeriod;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
