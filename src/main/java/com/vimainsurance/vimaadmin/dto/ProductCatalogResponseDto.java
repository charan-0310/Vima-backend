package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogResponseDto {

    private UUID id;
    private UUID organizationId;
    private String productType;
    private String name;
    private Boolean isMandatory;
    private String pricingModel;
    private String coverageOptions;
    private String coveredRelationships;
    private Integer displayOrder;
    private Long policyId;
    private String gradeFilter;
    private Boolean isActive;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
