package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessPartnerOrgResponseDto {

    private UUID id;
    private UUID partnerId;
    private UUID organizationId;
    private Boolean isActive;
    private Integer displayOrder;
    private String customRedirectUrl;
    private Map<String, Object> config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String partnerName;
    private String partnerSlug;
    private String partnerCategory;
    private String partnerRedirectType;
}
