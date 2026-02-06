package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class EnrollmentSubmissionResponseDto {

    private UUID id;
    private UUID employeeId;
    private UUID enrollmentWindowId;
    private UUID invitationId;
    private UUID endorsementId;
    private String referenceNumber;
    private String status;
    private String planSelections;
    private String nomineeData;
    private String personalDetails;
    private String dependents;
    private String stage;
    private String premiumBreakdown;
    private LocalDateTime submittedAt;
    private UUID reviewedById;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private Boolean declarationAccepted;
    private LocalDateTime declarationTimestamp;
    private String declarationIpAddress;
    private Integer version;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
