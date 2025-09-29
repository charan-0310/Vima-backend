package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;

import com.vimainsurance.vimaadmin.entity.IncentiveRuleSlab;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IncentiveRuleSlabDto {
    private Long id;
    private Long ruleId;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal payoutAmount;

    public IncentiveRuleSlabDto(IncentiveRuleSlab entity) {
        this.id = entity.getId();
        this.ruleId = entity.getRule() != null ? entity.getRule().getId() : null;
        this.minValue = entity.getMinValue();
        this.maxValue = entity.getMaxValue();
        this.payoutAmount = entity.getPayoutAmount();
    }
} 