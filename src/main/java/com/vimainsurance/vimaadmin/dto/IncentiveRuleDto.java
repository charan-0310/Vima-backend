package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.entity.IncentiveRule;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IncentiveRuleDto {
    private Long id;
    private Long packageId;
    private IncentiveRule.RuleType ruleType;
    private BigDecimal fixedPayout;
    private OffsetDateTime createdAt;
    private List<IncentiveRuleSlabDto> slabs;

    public IncentiveRuleDto(IncentiveRule entity) {
        this.id = entity.getId();
        this.packageId = entity.getPackageEntity() != null ? entity.getPackageEntity().getId() : null;
        this.ruleType = entity.getRuleType();
        this.fixedPayout = entity.getFixedPayout();
        this.createdAt = entity.getCreatedAt();
        if (entity.getSlabs() != null) {
            this.slabs = entity.getSlabs().stream()
                    .map(IncentiveRuleSlabDto::new)
                    .collect(Collectors.toList());
        }
    }
} 