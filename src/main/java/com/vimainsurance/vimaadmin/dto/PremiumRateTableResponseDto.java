package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.RateSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumRateTableResponseDto {

    private UUID id;
    private UUID companyId;
    private Long policyId;
    private String productType;
    private String memberType;
    private Integer ageBandMin;
    private Integer ageBandMax;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PricingModel pricingModel;
    private BigDecimal sumInsuredAmount;
    private Integer familySizeMin;
    private Integer familySizeMax;
    private RateSource rateSource;
    private Boolean gstInclusive;
    private BigDecimal gstPercentage;
}
