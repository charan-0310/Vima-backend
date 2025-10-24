package com.vimainsurance.vimaadmin.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Policy entity representing insurance policies
 */
@Entity
@Table(name = "policies", schema = "cpc")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "policy_number", nullable = false, unique = true, length = 100)
    private String policyNumber;

    // Relationships
    @Column(name = "primary_individual_id", nullable = false)
    private UUID primaryIndividualId;

    @Column(name = "insurance_provider_id", nullable = false)
    private UUID insuranceProviderId;

    @Column(name = "insurance_product_id")
    private UUID insuranceProductId;

    @Column(name = "organization_id")
    private UUID organizationId;

    // Policy Details
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "coverage_type", nullable = false)
    private CoverageType coverageType = CoverageType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    // Coverage
    @Column(name = "covered_individuals", columnDefinition = "uuid[]")
    private List<UUID> coveredIndividuals;

    @Column(name = "sum_insured", nullable = false, precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "premium_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    // Dates
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    // Origin
    @Column(name = "lead_id")
    private UUID leadId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Policy() {
    }

    public Policy(String policyNumber, UUID primaryIndividualId, UUID insuranceProviderId, 
                 ProductType productType, CoverageType coverageType, PolicyStatus status,
                 BigDecimal sumInsured, BigDecimal premiumAmount, LocalDate startDate, LocalDate endDate) {
        this.policyNumber = policyNumber;
        this.primaryIndividualId = primaryIndividualId;
        this.insuranceProviderId = insuranceProviderId;
        this.productType = productType;
        this.coverageType = coverageType;
        this.status = status;
        this.sumInsured = sumInsured;
        this.premiumAmount = premiumAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
