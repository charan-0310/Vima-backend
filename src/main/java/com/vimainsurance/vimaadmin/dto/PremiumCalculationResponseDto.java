package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    }
}
