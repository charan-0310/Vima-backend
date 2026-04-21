package com.vimainsurance.vimaadmin.mapper;

import java.time.LocalDateTime;
import java.util.HashMap;

import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerOrgResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerRequestDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerResponseDto;
import com.vimainsurance.vimaadmin.entity.WellnessPartner;
import com.vimainsurance.vimaadmin.entity.WellnessPartnerOrganization;

public final class WellnessPartnerMapper {

    private WellnessPartnerMapper() {
    }

    public static WellnessPartner mapToEntity(WellnessPartnerRequestDto dto) {
        if (dto == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        WellnessPartner entity = new WellnessPartner();
        entity.setSlug(dto.getSlug());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLongDescription(dto.getLongDescription());
        entity.setCategory(dto.getCategory());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setIconName(dto.getIconName());
        entity.setCardColor(dto.getCardColor());
        entity.setRedirectUrl(dto.getRedirectUrl());
        entity.setRedirectType(dto.getRedirectType());
        entity.setIsActive(true);
        entity.setMetadata(dto.getMetadata() != null ? dto.getMetadata() : new HashMap<>());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public static WellnessPartnerResponseDto mapToResponseDto(WellnessPartner entity) {
        return mapToResponseDto(entity, 0L);
    }

    public static WellnessPartnerResponseDto mapToResponseDto(WellnessPartner entity, long organizationCount) {
        if (entity == null) {
            return null;
        }

        return WellnessPartnerResponseDto.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .name(entity.getName())
                .description(entity.getDescription())
                .longDescription(entity.getLongDescription())
                .category(entity.getCategory())
                .logoUrl(entity.getLogoUrl())
                .iconName(entity.getIconName())
                .cardColor(entity.getCardColor())
                .redirectUrl(entity.getRedirectUrl())
                .redirectType(entity.getRedirectType())
                .isActive(entity.getIsActive())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .organizationCount(organizationCount)
                .build();
    }

    public static void updateEntityFromDto(WellnessPartner entity, WellnessPartnerRequestDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setSlug(dto.getSlug());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLongDescription(dto.getLongDescription());
        entity.setCategory(dto.getCategory());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setIconName(dto.getIconName());
        entity.setCardColor(dto.getCardColor());
        entity.setRedirectUrl(dto.getRedirectUrl());
        entity.setRedirectType(dto.getRedirectType());
        entity.setMetadata(dto.getMetadata() != null ? dto.getMetadata() : new HashMap<>());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public static WellnessPartnerOrganization mapOrgRequestToEntity(WellnessPartnerOrgRequestDto dto) {
        if (dto == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        WellnessPartnerOrganization entity = new WellnessPartnerOrganization();
        entity.setPartnerId(dto.getPartnerId());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        entity.setCustomRedirectUrl(dto.getCustomRedirectUrl());
        entity.setConfig(dto.getConfig() != null ? dto.getConfig() : new HashMap<>());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public static WellnessPartnerOrgResponseDto mapToOrgResponseDto(WellnessPartnerOrganization entity) {
        if (entity == null) {
            return null;
        }

        WellnessPartner partner = entity.getPartner();
        return WellnessPartnerOrgResponseDto.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .organizationId(entity.getOrganizationId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .customRedirectUrl(entity.getCustomRedirectUrl())
                .config(entity.getConfig())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .partnerName(partner != null ? partner.getName() : null)
                .partnerSlug(partner != null ? partner.getSlug() : null)
                .partnerCategory(partner != null ? partner.getCategory() : null)
                .partnerRedirectType(partner != null ? partner.getRedirectType() : null)
                .build();
    }

    public static void updateOrgEntityFromDto(WellnessPartnerOrganization entity, WellnessPartnerOrgRequestDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setPartnerId(dto.getPartnerId());
        entity.setOrganizationId(dto.getOrganizationId());
        if (dto.getDisplayOrder() != null) {
            entity.setDisplayOrder(dto.getDisplayOrder());
        }
        entity.setCustomRedirectUrl(dto.getCustomRedirectUrl());
        entity.setConfig(dto.getConfig() != null ? dto.getConfig() : new HashMap<>());
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }
}
