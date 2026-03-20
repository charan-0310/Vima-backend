package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationRequestDto {

    @NotNull
    @Valid
    private List<PlanSelectionItemDto> planSelections;

    @Valid
    private List<DependentItemDto> dependents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSelectionItemDto {
        @NotNull
        private String planType;
        @Builder.Default
        private Boolean opted = true;
        private BigDecimal sumInsured;
        private String coverageTier;
        /** Product catalog id for TOP_UP / SUPER_TOP_UP; used to resolve admin-defined SI–premium pairs on policy. */
        private UUID topupPlanOptionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependentItemDto {
        private String name;
        private String relationship;
        private LocalDate dateOfBirth;
    }
}
