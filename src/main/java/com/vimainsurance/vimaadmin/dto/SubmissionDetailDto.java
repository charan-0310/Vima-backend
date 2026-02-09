package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for enrollment submission detail (HR portal).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetailDto {

    private UUID id;
    private String referenceNumber;
    private String status;
    private String stage;
    private String planSelections;
    private String nomineeData;
    private String personalDetails;
    private String dependents;
    private String premiumBreakdown;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
    private UUID reviewedById;
    private String rejectionReason;
    private Boolean declarationAccepted;
    private LocalDateTime declarationTimestamp;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Employee (primary) info
    private UUID employeeId;
    private String employeeName;
    private String employeeEmail;
    private String employeePhone;
    private String employeeNumber;

    // Window & org
    private UUID enrollmentWindowId;
    private String windowName;
    private UUID organizationId;
    private String organizationName;

    // Invitation (if any)
    private UUID invitationId;
    private String invitationStatus;
}
