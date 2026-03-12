package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkEmployeePolicyMapRequestDto {

    @NotNull
    private UUID organizationId;

    @NotNull
    private Long policyId;

    @NotNull
    @Valid
    @NotEmpty
    private List<IndividualMappingDto> individuals;

    @Data
    public static class IndividualMappingDto {
        @NotNull
        private UUID individualId;

        private UUID primaryEmployeeId;

        @NotBlank
        private String relationship;

        private BigDecimal sumInsured;

        private String coverageTier;
    }
}
