package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.ProductCatalogRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogResponseDto;
import com.vimainsurance.vimaadmin.entity.ProductCatalog;

public final class ProductCatalogMapper {

    private ProductCatalogMapper() {
    }

    public static ProductCatalog toEntity(ProductCatalogRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return ProductCatalog.builder()
                .organizationId(dto.getOrganizationId())
                .productType(dto.getProductType())
                .name(dto.getName())
                .isMandatory(dto.getIsMandatory() != null ? dto.getIsMandatory() : false)
                .pricingModel(dto.getPricingModel())
                .coverageOptions(dto.getCoverageOptions())
                .premiumPreviewOptions(dto.getPremiumPreviewOptions())
                .coveredRelationships(dto.getCoveredRelationships())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .policyId(dto.getPolicyId())
                .gradeFilter(dto.getGradeFilter())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .build();
    }

    public static ProductCatalogResponseDto toResponseDto(ProductCatalog entity) {
        if (entity == null) {
            return null;
        }
        return ProductCatalogResponseDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .productType(entity.getProductType())
                .name(entity.getName())
                .isMandatory(entity.getIsMandatory())
                .pricingModel(entity.getPricingModel())
                .coverageOptions(entity.getCoverageOptions())
                .premiumPreviewOptions(entity.getPremiumPreviewOptions())
                .coveredRelationships(entity.getCoveredRelationships())
                .displayOrder(entity.getDisplayOrder())
                .policyId(entity.getPolicyId())
                .gradeFilter(entity.getGradeFilter())
                .isActive(entity.getIsActive())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
