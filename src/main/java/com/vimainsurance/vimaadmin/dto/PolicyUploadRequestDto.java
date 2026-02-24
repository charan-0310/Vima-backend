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
}
