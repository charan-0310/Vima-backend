package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IncentiveEvaluationResponseDto {
    private Long packageId;
    private String packageName;
    private BigDecimal totalPayout;
    private List<RuleEvaluationDto> ruleEvaluations;

    public IncentiveEvaluationResponseDto(Long packageId, String packageName, BigDecimal totalPayout, List<RuleEvaluationDto> ruleEvaluations) {
        this.packageId = packageId;
        this.packageName = packageName;
        this.totalPayout = totalPayout;
        this.ruleEvaluations = ruleEvaluations;
    }

    // Getters and setters
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public BigDecimal getTotalPayout() { return totalPayout; }
    public void setTotalPayout(BigDecimal totalPayout) { this.totalPayout = totalPayout; }
    public List<RuleEvaluationDto> getRuleEvaluations() { return ruleEvaluations; }
    public void setRuleEvaluations(List<RuleEvaluationDto> ruleEvaluations) { this.ruleEvaluations = ruleEvaluations; }

    public static class RuleEvaluationDto {
        private Long ruleId;
        private String ruleType;
        private BigDecimal payout;
        private String appliedSlab;

        public RuleEvaluationDto() {}

        public RuleEvaluationDto(Long ruleId, String ruleType, BigDecimal payout, String appliedSlab) {
            this.ruleId = ruleId;
            this.ruleType = ruleType;
            this.payout = payout;
            this.appliedSlab = appliedSlab;
        }

        // Getters and setters
        public Long getRuleId() { return ruleId; }
        public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
        public String getRuleType() { return ruleType; }
        public void setRuleType(String ruleType) { this.ruleType = ruleType; }
        public BigDecimal getPayout() { return payout; }
        public void setPayout(BigDecimal payout) { this.payout = payout; }
        public String getAppliedSlab() { return appliedSlab; }
        public void setAppliedSlab(String appliedSlab) { this.appliedSlab = appliedSlab; }
    }
} 