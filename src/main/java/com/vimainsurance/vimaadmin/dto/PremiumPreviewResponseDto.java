package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;

import com.vimainsurance.vimaadmin.enums.RateSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPreviewResponseDto {

    private BigDecimal totalPremium;
    private BigDecimal gstAmount;
    private RateSource rateSource;
    private String matchedAgeBand;
    private Integer matchedAgeBandMin;
    private Integer matchedAgeBandMax;
}
