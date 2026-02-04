package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.EnrollmentWindowRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

public class EnrollmentWindowMapper {

    public static EnrollmentWindows mapToEntity(EnrollmentWindowRequestDto dto, Organization organization, AdminUser createdBy) {
        EnrollmentWindows entity = new EnrollmentWindows();
        entity.setOrganization(organization);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStatus(dto.getStatus() != null ? EnrollementStatus.fromValue(dto.getStatus()) : EnrollementStatus.SCHEDULED);
        entity.setConfig(dto.getConfig() != null ? dto.getConfig() : "{}");
        entity.setCreatedBy(createdBy);
        return entity;
    }

    public static void updateEntityFromDto(EnrollmentWindows entity, EnrollmentWindowRequestDto dto,
            Organization organization) {
        if (organization != null) {
            entity.setOrganization(organization);
        }
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getStartDate() != null) {
            entity.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            entity.setEndDate(dto.getEndDate());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(EnrollementStatus.fromValue(dto.getStatus()));
        }
        if (dto.getConfig() != null) {
            entity.setConfig(dto.getConfig());
        }
    }

    public static EnrollmentWindowResponseDto mapToResponseDto(EnrollmentWindows entity) {
        EnrollmentWindowResponseDto dto = new EnrollmentWindowResponseDto();
        dto.setId(entity.getId());
        if (entity.getOrganization() != null) {
            dto.setOrganizationId(entity.getOrganization().getOrganizationId());
            dto.setOrganizationName(entity.getOrganization().getOrganizationName());
        }
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().getValue() : null);
        dto.setConfig(entity.getConfig());
        if (entity.getCreatedBy() != null) {
            dto.setCreatedBy(entity.getCreatedBy().getId());
            dto.setCreatedByUsername(entity.getCreatedBy().getUsername());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setClosedAt(entity.getClosedAt());
        return dto;
    }
}
