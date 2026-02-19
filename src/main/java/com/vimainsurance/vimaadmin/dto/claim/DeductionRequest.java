package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionRequest {
    private String deductionDetails;
    private BigDecimal deductionAmount;
}
