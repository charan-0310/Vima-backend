package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationResponseDto {
    private UUID organizationId;
    private String organizationName;
    private String gstin;
    private String panNumber;
    private String primaryContactName;
    private String primaryContactEmail;
    private String primaryContactPhone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String registeredAddress;
    private String industry;
    private long employeesCount;
    private long policyCount;
    private BigDecimal totalPremiumAmount;
}


