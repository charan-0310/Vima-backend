package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class EndorsementResponseDto {
    private UUID endorsementId;
    private UUID organizationId;
    private String organizationName;
    /** User-facing label (may differ from legal organizationName). */
    private String organizationDisplayName;
    private UUID documentId;
    private String endorsementType;
    private String status;
    private Integer totalEmployees;
    private Integer totalDependents;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private UUID uploadedBy;
    private String uploadedByName;
    private String confirmationMethod;
    private String insurerRefNumber;
    private String premiumChangeType;
    private BigDecimal premiumAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String source;
    private String lifeEventType;
    /** Self-enrollment / window-scoped batches. */
    private UUID enrollmentWindowId;
    private String enrollmentWindowName;
    private Long policyId;
    private String policyType;
    /** Policy number for UI (list/detail); populated without loading full {@code Policy} entity on filtered list. */
    private String policyNumber;
    /** Optional display label (e.g. policy description when set). */
    private String policyName;
    private String insuranceCompanyName;
    /** CD account balance for the policy's linked CD account (v2), when available. */
    private BigDecimal cdBalance;
    private UUID splitGroupId;
    private UUID parentEndorsementId;
}

