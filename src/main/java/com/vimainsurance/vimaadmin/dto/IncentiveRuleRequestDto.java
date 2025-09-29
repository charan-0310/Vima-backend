package com.vimainsurance.vimaadmin.dto;

import com.vimainsurance.vimaadmin.entity.IncentiveRule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncentiveRuleRequestDto {
    @NotNull(message = "Package ID is required")
    private Long packageId;
    
    @NotNull(message = "Rule type is required")
    private IncentiveRule.RuleType ruleType;
    
    @DecimalMin(value = "0.0", message = "Fixed payout must be non-negative")
    private BigDecimal fixedPayout;
    
    private List<IncentiveRuleSlabRequestDto> slabs;
} 