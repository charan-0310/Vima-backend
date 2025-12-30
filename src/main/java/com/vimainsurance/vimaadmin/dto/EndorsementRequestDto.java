package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class EndorsementRequestDto {
    private UUID endorsementId; // for update operations

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    private UUID documentId;

    @NotNull(message = "Endorsement type is required")
    private String endorsementType; // ADDITION or DELETION

    private String status; // PENDING_APPROVAL, ACTIVE, etc.

    @Min(value = 0, message = "Total employees must be non-negative")
    private Integer totalEmployees = 0;

    @Min(value = 0, message = "Total dependents must be non-negative")
    private Integer totalDependents = 0;

    private String approvedBy; // AdminUser Username

    private UUID uploadedBy; // AdminUser ID

    private String confirmationMethod; // EMAIL, PORTAL, API

    private String insurerRefNumber;

    private String premiumChangeType; // INCREASE, DECREASE, NO_CHANGE

    private BigDecimal premiumAmount;
}

