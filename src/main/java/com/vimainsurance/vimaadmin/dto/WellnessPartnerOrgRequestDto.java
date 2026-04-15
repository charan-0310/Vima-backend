package com.vimainsurance.vimaadmin.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessPartnerOrgRequestDto {

    @NotNull(message = "partnerId is required")
    private UUID partnerId;

    @NotNull(message = "organizationId is required")
    private UUID organizationId;

    private Integer displayOrder;

    private String customRedirectUrl;

    private Map<String, Object> config;

    @Builder.Default
    private Boolean isActive = true;
}
