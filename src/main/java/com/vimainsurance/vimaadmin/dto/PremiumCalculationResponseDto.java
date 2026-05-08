package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationResponseDto {

    private BigDecimal totalAnnualPremium;
    private BigDecimal totalEmployerShare;
    private BigDecimal totalEmployeeShare;
    private BigDecimal gstAmount;
    private List<PlanBreakdownItemDto> perPlanBreakdown;
    private Map<String, BigDecimal> deductionOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanBreakdownItemDto {
        private String planType;
        private BigDecimal premium;
        private BigDecimal employerShare;
        private BigDecimal employeeShare;
        private BigDecimal gstAmount;
        /**
         * The cost-sharing rule that produced this split, if any.
         * Null when no rule matched (default 100% employer applied) or when the plan has
         * special-cased logic (TOP_UP / SUPER_TOP_UP override). Surfaced for HR debugging
         * so they can see "which row in my rate card actually fired for this employee".
         */
        private UUID appliedRuleId;
        /**
         * The coverage_category of the rule that produced this split. Useful when the
         * applied rule is NOT the resolved category (e.g. resolver asked for ALL_DEPENDENTS
         * but the lookup landed on SPOUSE via the legacy fallback). Helps HR spot config
         * gaps: "an ALL_DEPENDENTS lookup landed on SPOUSE — should I add a DEFAULT rule?".
         */
        private String appliedCategory;
    }
}
