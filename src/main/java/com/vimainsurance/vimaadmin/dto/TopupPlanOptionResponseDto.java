package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupPlanOptionResponseDto {

    private UUID id;
    private UUID companyId;
    private Long policyId;
    private String planType;
    private String name;
    private String description;
    private String insurerName;
    private BigDecimal deductibleAmount;
    private List<BigDecimal> sumInsuredOptions;
    private String pricingModel;
    private Boolean coversDependents;
    private Boolean coversParents;
    private Boolean isActive;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
