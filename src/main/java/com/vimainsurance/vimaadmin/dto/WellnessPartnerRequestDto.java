package com.vimainsurance.vimaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessPartnerRequestDto {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "slug is required")
    private String slug;

    private String description;

    private String longDescription;

    @NotBlank(message = "category is required")
    private String category;

    private String logoUrl;

    private String iconName;

    private String cardColor;

    private String redirectUrl;

    @NotNull(message = "redirectType is required")
    private String redirectType;

    private Map<String, Object> metadata;
}
