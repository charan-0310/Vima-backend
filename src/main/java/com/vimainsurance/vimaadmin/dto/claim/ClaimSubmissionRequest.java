package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimCategory;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.MemberType;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.Relationship;
import com.vimainsurance.vimaadmin.enums.SubmissionSource;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimSubmissionRequest {

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Policy ID is required")
    private Long policyId;

    /** Display name / logo label for the insurance provider (e.g. "Star Health"). */
    private String insuranceProviderLogo;
    /** Policy number (e.g. "GMC-2026-CHN-00123"). */
    private String policyNumber;
    /** Policy validity end date (ISO date). */
    private LocalDate validUntil;

    private String memberId;
    private MemberType memberType;
    private String memberName;
    private LocalDate memberDob;
    private String memberUhid;
    private Relationship relationship;

    private ClaimType claimType;
    private ClaimCategory claimCategory;
    private ProductType productType;
    private String reasonForAdmission;
    private String diagnosis;

    @DecimalMin(value = "0.01", message = "Claim amount must be greater than 0")
    private BigDecimal claimAmount;

    private String hospitalName;
    private String hospitalCity;
    private String hospitalState;
    private String hospitalPincode;
    private String hospitalProviderCode;
    private Boolean isNetworkHospital;

    private LocalDate dateOfAdmission;
    private LocalDate dateOfDischarge;

    private String bankAccountNumber;
    private String accountHolderName;
    private String ifscCode;
    private String bankBranchName;

    /** For FOLLOW_UP claims: parent claim UUID */
    private UUID parentClaimId;
    private String parentInsurerClaimRef;

    private String abhaId;
    private SubmissionSource submissionSource;
}
