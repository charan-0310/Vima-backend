package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Policy response
 */
@Data
public class PolicyResponseDto {

    private Long policyId;
    private String policyNumber;
    private UUID primaryIndividualId;
    private String insuranceProvider;
    private UUID insuranceProductId;
    private UUID organizationId;
    private String productType;
    private String coverageType;
    private String status;
    private List<UUID> coveredIndividuals;
    private BigDecimal sumInsured;
    private BigDecimal premiumAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate renewalDate;

    private UUID leadId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<DealsResponseDto> dependents;

    private String paymentFrequency;
}
