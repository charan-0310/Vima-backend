package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.PremiumRateTableRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableResponseDto;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;

public final class PremiumRateTableMapper {

    private PremiumRateTableMapper() {
    }

    public static PremiumRateTable toEntity(PremiumRateTableRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return PremiumRateTable.builder()
                .organizationId(dto.getCompanyId())
                .policyId(dto.getPolicyId())
                .productType(dto.getProductType())
                .memberType(dto.getMemberType())
                .ageBandMin(dto.getAgeBandMin())
                .ageBandMax(dto.getAgeBandMax())
                .rate(dto.getRate())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .pricingModel(dto.getPricingModel())
                .sumInsuredAmount(dto.getSumInsuredAmount())
                .familySizeMin(dto.getFamilySizeMin())
                .familySizeMax(dto.getFamilySizeMax())
                .rateSource(dto.getRateSource())
                .gstInclusive(Boolean.TRUE.equals(dto.getGstInclusive()))
                .gstPercentage(dto.getGstPercentage())
                .isDeleted(false)
                .build();
    }

    public static PremiumRateTableResponseDto toResponseDto(PremiumRateTable entity) {
        if (entity == null) {
            return null;
        }
        return PremiumRateTableResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getOrganizationId())
                .policyId(entity.getPolicyId())
                .productType(entity.getProductType())
                .memberType(entity.getMemberType())
                .ageBandMin(entity.getAgeBandMin())
                .ageBandMax(entity.getAgeBandMax())
                .rate(entity.getRate())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .pricingModel(entity.getPricingModel())
                .sumInsuredAmount(entity.getSumInsuredAmount())
                .familySizeMin(entity.getFamilySizeMin())
                .familySizeMax(entity.getFamilySizeMax())
                .rateSource(entity.getRateSource())
                .gstInclusive(entity.getGstInclusive())
                .gstPercentage(entity.getGstPercentage())
                .build();
    }
}
