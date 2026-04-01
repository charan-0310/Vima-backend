package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

/**
 * DTO for Policy response
 */
@Data
public class PolicyResponseDto {

    private Long policyId;
    private String policyNumber;
    private UUID primaryIndividualId;
    /** Display name from insurance_providers.provider_name */
    private String insuranceProvider;
    /** Provider code (e.g. ICICI) for form selects; same as insurance_company_code on write */
    private String insuranceProviderCode;
    private UUID insuranceProductId;
    private UUID organizationId;
    private UUID documentId;

    // Policy Type and Category
    private String productType; // GMC, GPA, GTL
    private String policyCategory; // EMPLOYEE, MOTOR, PROPERTY, LIABILITY
    private Boolean appliesToEmployees;


    private String coverageType; // E, ES, ESC, ESCP (for GMC) or INDIVIDUAL, FAMILY_FLOATER, GROUP (traditional)
    private Integer maxChildrenAllowed;
    private String status;
    private List<UUID> coveredIndividuals;
    private BigDecimal sumInsured;
    private Integer sumInsuredMultiplier; // For GPA/GTL
    private BigDecimal premiumAmount;
    private BigDecimal netAmount;
    private BigDecimal gst;
    private UUID cdAccountId;
    private BigDecimal cdBalance;
    private Long employeesCount;
    private Long dependentsCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate renewalDate;

    private UUID leadId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<DealsResponseDto> dependents;

    private List<NomineeResponseDto> nominees;

    private MotorPolicyDetailsResponseDto motorPolicyDetails;

    private String paymentFrequency;

    // TPA Details
    private String tpaOrganizationName;
    private String tpaContactInfo;

    // PARENT_GMC: Parent/In-Law
    private Boolean parentCoverageEnabled;
    private Boolean inLawCoverageEnabled;
    private Integer maxParents;
    private Integer maxInLaws;
    private Integer parentAgeLimit;

    // TOP_UP / SUPER_TOP_UP
    private String description;
    private String insurerName;
    private BigDecimal deductibleAmount;
    private String sumInsuredOptions; // JSON array string
    /** JSON array string of annual premiums; index-aligned with sumInsuredOptions. */
    private String topupPremiumOptions;
    private Boolean coversDependents;
    private Boolean coversParents;
    private Boolean isDeleted;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveTo;
}
