package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.CostSharingRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.CostSharingRuleResponseDto;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;

public final class CostSharingRuleMapper {

    private CostSharingRuleMapper() {
    }

    public static CostSharingRule toEntity(CostSharingRuleRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return CostSharingRule.builder()
                .companyId(dto.getCompanyId())
                .planType(dto.getPlanType())
                .coverageCategory(dto.getCoverageCategory())
                .employerShareType(dto.getEmployerShareType())
                .employerShareValue(dto.getEmployerShareValue())
                .excessAllowed(Boolean.TRUE.equals(dto.getExcessAllowed()))
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .isDeleted(false)
                .build();
    }

    public static CostSharingRuleResponseDto toResponseDto(CostSharingRule entity) {
        if (entity == null) {
            return null;
        }
        return CostSharingRuleResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .planType(entity.getPlanType())
                .coverageCategory(entity.getCoverageCategory())
                .employerShareType(entity.getEmployerShareType())
                .employerShareValue(entity.getEmployerShareValue())
                .excessAllowed(entity.getExcessAllowed())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
