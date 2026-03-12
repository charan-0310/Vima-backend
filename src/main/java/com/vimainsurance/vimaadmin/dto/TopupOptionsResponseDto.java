package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Employee-facing response: list of active top-up options with premium preview per sum-insured.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupOptionsResponseDto {

    private List<TopupOptionWithPreview> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopupOptionWithPreview {
        private UUID id;
        private String planType;
        private String name;
        private BigDecimal deductibleAmount;
        private List<BigDecimal> sumInsuredOptions;
        /** Sum insured -> annual premium for preview. */
        private Map<BigDecimal, BigDecimal> premiumPreviewPerOption;
    }
}
