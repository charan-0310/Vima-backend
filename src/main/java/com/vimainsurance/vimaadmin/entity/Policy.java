package com.vimainsurance.vimaadmin.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.PaymentFrequency;
import com.vimainsurance.vimaadmin.enums.ProductType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @Column(name = "primary_individual_id")
    private UUID primaryIndividualId;

    @Column(name = "insurance_provider_id", nullable = false)
    private UUID insuranceProviderId;

    @Column(name = "insurance_product_id")
    private UUID insuranceProductId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "document_id")
    private Document document;

    // Policy Category (stored as VARCHAR in DB, mapped to PolicyCategory enum)
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_category", nullable = false, length = 50)
    private ProductType policyCategory = ProductType.EMPLOYEE;

    @Column(name = "applies_to_employees", nullable = false)
    private Boolean appliesToEmployees = true;

    // TPA Details (GMC only)
    @Column(name = "tpa_organization_name")
    private String tpaOrganizationName;

    @Column(name = "tpa_contact_info")
    private String tpaContactInfo;


    // CTC Multiplier (GPA/GTL only)
    @Column(name = "sum_insured_multiplier")
    private Integer sumInsuredMultiplier;

    // Policy Details
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "product_type", nullable = false) // Policy_type
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "coverage_type")
    private CoverageType coverageType = CoverageType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    // Coverage
    @Column(name = "covered_individuals", columnDefinition = "uuid[]")
    private List<UUID> coveredIndividuals;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Nominee> nominees = new ArrayList<>();

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MotorPolicyDetails motorPolicyDetails;

    @Column(name = "sum_insured", precision = 15, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "total_premium_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "gst", nullable = false, precision = 15, scale = 2)
    private BigDecimal gst;

    @Column(name = "cd_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal cdBalance = BigDecimal.ZERO;

    // Dates
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    // PARENT_GMC: Parent/In-Law coverage fields
    @Column(name = "parent_coverage_enabled")
    private Boolean parentCoverageEnabled;

    @Column(name = "in_law_coverage_enabled")
    private Boolean inLawCoverageEnabled;

    @Column(name = "max_parents")
    private Integer maxParents;

    @Column(name = "max_in_laws")
    private Integer maxInLaws;

    @Column(name = "parent_age_limit")
    private Integer parentAgeLimit;

    // TOP_UP / SUPER_TOP_UP fields
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "insurer_name", length = 255)
    private String insurerName;

    @Column(name = "deductible_amount", precision = 15, scale = 2)
    private BigDecimal deductibleAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sum_insured_options", columnDefinition = "jsonb")
    private String sumInsuredOptions; // JSON array of numbers

    /** JSON array of numbers; index-aligned with sumInsuredOptions for TOP_UP / SUPER_TOP_UP. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topup_premium_options", columnDefinition = "jsonb")
    private String topupPremiumOptions;

    @Column(name = "covers_dependents")
    private Boolean coversDependents;

    @Column(name = "covers_parents")
    private Boolean coversParents;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_frequency")
    private PaymentFrequency paymentFrequency = PaymentFrequency.YEARLY;

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
