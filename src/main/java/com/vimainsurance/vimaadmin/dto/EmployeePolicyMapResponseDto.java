package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePolicyMapResponseDto {

    private UUID id;
    private UUID individualId;
    private String individualName;
    private UUID primaryEmployeeId;
    private String primaryEmployeeName;
    private String relationship;
    private Long policyId;
    private String policyNumber;
    private String productType;
    private String insurerName;
    private UUID organizationId;
    private BigDecimal sumInsured;
    private String coverageTier;
    private Boolean isVoluntary;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String source;
    private LocalDateTime createdAt;
}
