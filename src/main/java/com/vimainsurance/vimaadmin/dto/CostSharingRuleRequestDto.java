package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.enums.EmployerShareType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostSharingRuleRequestDto {

    @NotNull
    private UUID companyId;

    @NotNull
    private String planType;

    @NotNull
    private CoverageCategory coverageCategory;

    @NotNull
    private EmployerShareType employerShareType;

    @NotNull
    private BigDecimal employerShareValue;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Builder.Default
    private Boolean excessAllowed = false;
}
