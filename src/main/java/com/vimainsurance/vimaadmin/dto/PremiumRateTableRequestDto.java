package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.RateSource;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumRateTableRequestDto {

    @NotNull
    private UUID companyId;

    private Long policyId;

    @NotNull
    private String productType;

    private String memberType;

    private Integer ageBandMin;
    private Integer ageBandMax;

    @NotNull
    private BigDecimal rate;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private PricingModel pricingModel;
    private BigDecimal sumInsuredAmount;
    private Integer familySizeMin;
    private Integer familySizeMax;
    private RateSource rateSource;

    @Builder.Default
    private Boolean gstInclusive = false;

    private BigDecimal gstPercentage;
}
