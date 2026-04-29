package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimCategory;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.MemberType;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.Relationship;
import com.vimainsurance.vimaadmin.enums.SubmissionSource;

import lombok.Data;

@Data
public class ClaimDetailsResponse {

    private UUID id;
    private String claimNumber;
    private UUID organizationId;
    private String organizationName;
    /** User-facing label (may differ from legal organizationName). */
    private String organizationDisplayName;
    private Long policyId;
    private String policyNumber;
    private String insuranceProviderLogo;
    private LocalDate validUntil;
    private UUID employeeId;
    private String employeeName;

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
    private BigDecimal claimAmount;

    private String hospitalName;
    private String hospitalCity;
    private String hospitalState;
    private String hospitalPincode;
    private String hospitalProviderCode;
    private Boolean isNetworkHospital;

    private LocalDate dateOfAdmission;
    private LocalDate dateOfDischarge;
    private LocalDate dateOfSubmission;

    private String bankAccountNumber;
    private String accountHolderName;
    private String ifscCode;
    private String bankBranchName;

    private UUID insurerId;
    private String insurerClaimRef;
    private String insurerClaimNumber;
    private String insurerInwardNumber;
    private String insurerStatus;
    private String insurerCurrentStatus;
    private String insurerRemarks;
    private String rejectionReason;

    private ClaimStatus internalStatus;
    /** User-friendly label for internalStatus (e.g. "Pending Review"). */
    private String displayStatus;
    /** Number of documents attached to this claim. */
    private Integer documentCount;
    private SubmissionSource submissionSource;
    private UUID parentClaimId;
    private String parentInsurerClaimRef;
    private String abhaId;

    private UUID submittedById;
    private UUID reviewedById;
    private LocalDateTime reviewedAt;
    private UUID approvedById;
    private LocalDateTime approvedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private Integer version;

    private ClaimSettlementDto settlement;
    private List<ClaimQueryDto> queries = new ArrayList<>();
    private List<ClaimDeductionDto> deductions = new ArrayList<>();
    private List<ClaimAuditLogDto> auditLogs = new ArrayList<>();
    private List<ClaimDocumentSummaryDto> documents = new ArrayList<>();

    @Data
    public static class ClaimSettlementDto {
        private UUID id;
        private BigDecimal claimedAmount;
        private BigDecimal grossSanctionedAmount;
        private BigDecimal netSanctionedAmount;
        private BigDecimal totalDisallowedAmount;
        private BigDecimal deductionAmount;
        private BigDecimal copayAmount;
        private BigDecimal amountPaid;
        private String paymentMode;
        private String chequeNumber;
        private LocalDate chequeDate;
        private LocalDate paymentDate;
        private String paymentReference;
        private LocalDate settlementDate;
    }

    @Data
    public static class ClaimQueryDto {
        private UUID id;
        private String insurerSysId;
        private String queryText;
        private LocalDate queryDate;
        private String queryStatus;
        private String responseText;
        private LocalDate responseDate;
        private String responseRemark;
        private String courierName;
        private String podNumber;
        private Integer numDocumentsAttached;
        private String employeeRemarks;
        private LocalDateTime employeeResponseAt;
    }

    @Data
    public static class ClaimDeductionDto {
        private UUID id;
        private String insurerSysId;
        private String deductionDetails;
        private BigDecimal deductionAmount;
    }

    @Data
    public static class ClaimAuditLogDto {
        private UUID id;
        private String action;
        private String oldStatus;
        private String newStatus;
        private String actorRole;
        private String details;
        private String correlationId;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ClaimDocumentSummaryDto {
        private UUID documentId;
        private String documentType;
        private String originalFilename;
        private LocalDateTime uploadedAt;
    }
}
