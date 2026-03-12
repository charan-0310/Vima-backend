package com.vimainsurance.vimaadmin.mapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionRequestDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionResponseDto;
import com.vimainsurance.vimaadmin.entity.TopupPlanOption;

public final class TopupPlanOptionMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<BigDecimal>> LIST_BIG_DECIMAL = new TypeReference<>() {};

    private TopupPlanOptionMapper() {
    }

    public static TopupPlanOption toEntity(TopupPlanOptionRequestDto dto) {
        if (dto == null) {
            return null;
        }
        String sumInsuredJson = toSumInsuredJson(dto.getSumInsuredOptions());
        return TopupPlanOption.builder()
                .companyId(dto.getCompanyId())
                .policyId(dto.getPolicyId())
                .planType(dto.getPlanType())
                .name(dto.getName())
                .description(dto.getDescription())
                .insurerName(dto.getInsurerName())
                .deductibleAmount(dto.getDeductibleAmount())
                .sumInsuredOptions(sumInsuredJson)
                .pricingModel(dto.getPricingModel())
                .coversDependents(dto.getCoversDependents() != null ? dto.getCoversDependents() : true)
                .coversParents(dto.getCoversParents() != null ? dto.getCoversParents() : false)
                .isActive(true)
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .isDeleted(false)
                .build();
    }

    public static TopupPlanOptionResponseDto toResponseDto(TopupPlanOption entity) {
        if (entity == null) {
            return null;
        }
        List<BigDecimal> sumInsured = fromSumInsuredJson(entity.getSumInsuredOptions());
        return TopupPlanOptionResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .policyId(entity.getPolicyId())
                .planType(entity.getPlanType())
                .name(entity.getName())
                .description(entity.getDescription())
                .insurerName(entity.getInsurerName())
                .deductibleAmount(entity.getDeductibleAmount())
                .sumInsuredOptions(sumInsured)
                .pricingModel(entity.getPricingModel())
                .coversDependents(entity.getCoversDependents())
                .coversParents(entity.getCoversParents())
                .isActive(entity.getIsActive())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static String toSumInsuredJson(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize sumInsuredOptions", e);
        }
    }

    public static List<BigDecimal> fromSumInsuredJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, LIST_BIG_DECIMAL);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse sumInsuredOptions: " + json, e);
        }
    }
}
