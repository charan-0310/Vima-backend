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
public class WellnessPartnerResponseDto {

    private UUID id;
    private String slug;
    private String name;
    private String description;
    private String longDescription;
    private String category;
    private String logoUrl;
    private String iconName;
    private String cardColor;
    private String redirectUrl;
    private String redirectType;
    private Boolean isActive;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long organizationCount;
}
