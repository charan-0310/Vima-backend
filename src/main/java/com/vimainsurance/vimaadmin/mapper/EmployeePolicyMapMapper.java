package com.vimainsurance.vimaadmin.mapper;

import java.time.LocalDate;

import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;

public final class EmployeePolicyMapMapper {

    private EmployeePolicyMapMapper() {
    }

    public static EmployeePolicyMapResponseDto toResponseDto(
            EmployeePolicyMap entity,
            String individualName,
            String primaryEmployeeName,
            String policyNumber,
            String productType,
            String insurerName) {
        return EmployeePolicyMapResponseDto.builder()
                .id(entity.getId())
                .individualId(entity.getIndividualId())
                .individualName(individualName)
                .primaryEmployeeId(entity.getPrimaryEmployeeId())
                .primaryEmployeeName(primaryEmployeeName)
                .relationship(entity.getRelationship())
                .policyId(entity.getPolicyId())
                .policyNumber(policyNumber)
                .productType(productType)
                .insurerName(insurerName)
                .organizationId(entity.getOrganizationId())
                .sumInsured(entity.getSumInsured())
                .coverageTier(entity.getCoverageTier())
                .isVoluntary(entity.getIsVoluntary())
                .status(entity.getStatus())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static EmployeePolicyMap toEntity(EmployeePolicyMapRequestDto dto) {
        LocalDate effectiveFrom = dto.getEffectiveFrom();
        return EmployeePolicyMap.builder()
                .individualId(dto.getIndividualId())
                .primaryEmployeeId(dto.getPrimaryEmployeeId())
                .relationship(dto.getRelationship())
                .policyId(dto.getPolicyId())
                .organizationId(dto.getOrganizationId())
                .sumInsured(dto.getSumInsured())
                .coverageTier(dto.getCoverageTier())
                .isVoluntary(Boolean.TRUE.equals(dto.getIsVoluntary()))
                .status("ACTIVE")
                .effectiveFrom(effectiveFrom)
                .effectiveTo(dto.getEffectiveTo())
                .source(dto.getSource() != null ? dto.getSource() : "MANUAL")
                .build();
    }
}
