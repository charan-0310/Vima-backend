package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Policy creation and update requests
 */
@Data
public class PolicyRequestDto {

    @NotBlank(message = "Policy number is required")
    @Size(max = 100, message = "Policy number must not exceed 100 characters")
    private String policyNumber;

    @NotNull(message = "Primary individual ID is required")
    private UUID primaryIndividualId;

    @NotNull(message = "Insurance Company Code is required")
    private String insuranceCompanyCode;

    private UUID insuranceProductId;

    private UUID organizationId;

    private UUID documentId;

    // Policy Type and Category
    @NotNull(message = "Product type is required")
    private String productType;// GMC, GPA, or GTL

    private String policyCategory; // EMPLOYEE, MOTOR, PROPERTY, LIABILITY (defaults to EMPLOYEE)

    private Boolean appliesToEmployees; // defaults to true



    // Coverage type - required for GMC (E, ES, ESC, ESCP), optional for traditional policies
    private String coverageType;
    /** Applies to GMC/GHI with ESC/ESCP. Range: 1..4. Defaults to 4 when omitted. */
    private Integer maxChildrenAllowed;


    private String status;

    private String coveredIndividuals;

    // Sum Insured - required for GMC, not used for GPA/GTL
    @DecimalMin(value = "0.0", inclusive = false, message = "Sum insured must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Sum insured must have at most 13 integer digits and 2 decimal places")
    private BigDecimal sumInsured;

    // CTC Multiplier - required for GPA/GTL (1-5x)
    @Min(value = 1, message = "Sum insured multiplier must be at least 1")
    @Max(value = 5, message = "Sum insured multiplier must be at most 5")
    private Integer sumInsuredMultiplier;

    @NotNull(message = "Premium amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Premium amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Premium amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal premiumAmount;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate renewalDate;

    private UUID leadId;

    private String providerCode;


    List<DealsRequestDto> dependents;

    private String paymentFrequency;

    private BigDecimal netAmount;

    private BigDecimal gst;

    // TPA Details - required for GMC only
    @Size(max = 255, message = "TPA Organization Name must not exceed 255 characters")
    private String tpaOrganizationName;

    @Size(max = 255, message = "TPA Contact Info must not exceed 255 characters")
    private String tpaContactInfo;

    // PARENT_GMC: Parent/In-Law
    private Boolean parentCoverageEnabled;
    private Boolean inLawCoverageEnabled;
    private Integer maxParents;
    private Integer maxInLaws;
    private Integer parentAgeLimit;

    // TOP_UP / SUPER_TOP_UP
    /** Pricing model for product catalog: FLAT, AGE_BANDED, FAMILY_FLOATER */
    private String pricingModel;
    private String description;
    private String insurerName;
    private BigDecimal deductibleAmount;
    private String sumInsuredOptions;
    /** JSON array string of annual premiums; same length/order as sumInsuredOptions for TOP_UP / SUPER_TOP_UP. */
    private String topupPremiumOptions;
    private Boolean coversDependents;
    private Boolean coversParents;
    private Boolean isDeleted;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveTo;

    /** Admin-authored condensed wording shown to employees. HTML from rich text editor. */
    @Size(max = 100_000, message = "Policy wording summary must not exceed 100000 characters")
    private String policyWordingSummary;

    /** Admin-authored "additional documents" rich text appended to the default claim checklist on the employee portal. */
    @Size(max = 100_000, message = "Claim checklist additional docs must not exceed 100000 characters")
    private String claimChecklistAdditionalDocs;
}
