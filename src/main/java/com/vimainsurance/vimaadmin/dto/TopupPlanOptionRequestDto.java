package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupPlanOptionRequestDto {

    private UUID companyId;

    private Long policyId;

    @NotBlank(message = "planType is required")
    private String planType; // TOP_UP | SUPER_TOP_UP

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private String insurerName;

    @NotNull(message = "deductibleAmount is required")
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal deductibleAmount;

    @NotNull(message = "sumInsuredOptions is required")
    private List<BigDecimal> sumInsuredOptions;

    @NotBlank(message = "pricingModel is required")
    private String pricingModel; // AGE_BANDED | FLAT

    private Boolean coversDependents;

    private Boolean coversParents;

    @NotNull(message = "effectiveFrom is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
