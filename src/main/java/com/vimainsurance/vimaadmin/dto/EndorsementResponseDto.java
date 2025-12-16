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
    private UUID documentId;
    private String endorsementType;
    private String status;
    private Integer totalEmployees;
    private Integer totalDependents;
    private LocalDateTime approvedAt;
    private UUID approvedBy;
    private String approvedByName;
    private UUID uploadedBy;
    private String uploadedByName;
    private String confirmationMethod;
    private String insurerRefNumber;
    private String premiumChangeType;
    private BigDecimal premiumAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

