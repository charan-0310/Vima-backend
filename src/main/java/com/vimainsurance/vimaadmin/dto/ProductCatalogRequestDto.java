package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.util.UUID;

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
public class ProductCatalogRequestDto {

    @NotNull(message = "organizationId is required")
    private UUID organizationId;

    @NotBlank(message = "productType is required")
    private String productType;

    @NotBlank(message = "name is required")
    private String name;

    @Builder.Default
    private Boolean isMandatory = false;

    private String pricingModel;

    private String coverageOptions;

    private String premiumPreviewOptions;

    private String coveredRelationships;

    @Builder.Default
    private Integer displayOrder = 0;

    private Long policyId;

    private String gradeFilter;

    @Builder.Default
    private Boolean isActive = true;

    @NotNull(message = "effectiveFrom is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
