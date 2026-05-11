package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class PolicyUploadRequestDto {
    private MultipartFile[] files;
    private String documentType;
    private String notes;
    private UUID individualId;
    
    // Policy Details
    private String policyNumber;
    private String insuranceCompany;
    private String productType;
    private String providerCode;
    private String coverageType;
    /** Applies to GMC/GHI with ESC/ESCP. Range: 1..4. Defaults to 4 when omitted. */
    private Integer maxChildrenAllowed;
    private String status;
    private BigDecimal sumInsured;
    /** For GPA/GTL: 1-5 when sumInsuredOption=MULTIPLIER; stored in sum_insured_multiplier. */
    private Integer sumInsuredMultiplier;
    /** For GPA/GTL only: MULTIPLIER (default) or FIXED. FIXED = use sumInsured as actual amount. */
    private String sumInsuredOption;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate renewalDate;
    private List<DealsRequestDto> dependents;
    private String paymentFrequency;
    private List<NomineeRequestDto> nominees;
    private MotorPolicyDetailsRequestDto motorDetails;
    private BigDecimal netAmount;
    private BigDecimal gst;

    // TPA Details
    private String tpaOrganizationName;
    private String tpaContactInfo;

    // PARENT_GMC: Parent/In-Law coverage fields
    private Boolean parentCoverageEnabled;
    private Boolean inLawCoverageEnabled;
    private Integer maxParents;
    private Integer maxInLaws;
    private Integer parentAgeLimit;

    // TOP_UP / SUPER_TOP_UP fields
    /** Pricing model for product catalog: FLAT, AGE_BANDED, FAMILY_FLOATER */
    private String pricingModel;
    private String description;
    private String insurerName;
    private BigDecimal deductibleAmount;
    /** Comma-separated or JSON array string, e.g. "500000,1000000" */
    private String sumInsuredOptions;
    /** Comma-separated or JSON array string; same count as sumInsuredOptions for TOP_UP / SUPER_TOP_UP. */
    private String topupPremiumOptions;
    /** Optional policy wording PDF uploaded alongside the policy on the create flow. Admin-only; not shown to employees. */
    private MultipartFile policyWordingFile;

    /** Optional claim checklist PDF uploaded alongside the policy on the create flow. Admin-only; not shown to employees. */
    private MultipartFile claimChecklistFile;

    /** Optional admin-authored condensed wording (HTML) shown to employees in place of the wording PDF. */
    private String policyWordingSummary;

    /** Optional admin-authored "additional documents" rich text (HTML) appended to the default claim checklist. */
    private String claimChecklistAdditionalDocs;

    private Boolean coversDependents;
    private Boolean coversParents;
    private Boolean isDeleted;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
