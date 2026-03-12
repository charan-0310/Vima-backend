package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeePolicyMapRequestDto {

    @NotNull
    private UUID individualId;

    private UUID primaryEmployeeId;

    @NotBlank
    private String relationship;

    @NotNull
    private Long policyId;

    @NotNull
    private UUID organizationId;

    private BigDecimal sumInsured;

    private String coverageTier;

    private Boolean isVoluntary;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String source;
}
