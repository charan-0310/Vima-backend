package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vimainsurance.vimaadmin.entity.IncentiveRule;
import com.vimainsurance.vimaadmin.entity.RuleTypeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncentiveRuleFullRequestDto {
    // @JsonDeserialize(using = RuleTypeDeserializer.class)
    private IncentiveRule.RuleType ruleType;
    private BigDecimal fixedPayout;
    private List<IncentiveRuleSlabRequestDto> slabs;
} 