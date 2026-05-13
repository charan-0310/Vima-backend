package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One missing (policy, product, member) combination for rate-card coverage at enrollment publish.
 * {@code memberType} is {@code SELF} for base product gaps; for {@code PARENT_GMC} it is {@code parent}
 * when age-banded parent/parent_in_law rate rows are required but absent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCardCoverageGapDto {

    private Long policyId;
    private String productType;
    private String memberType;
}
