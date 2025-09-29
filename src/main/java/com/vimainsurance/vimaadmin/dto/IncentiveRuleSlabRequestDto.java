package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncentiveRuleSlabRequestDto {
    @NotNull(message = "Min value is required")
    @DecimalMin(value = "0.0", message = "Min value must be non-negative")
    private BigDecimal minValue;
    
    private BigDecimal maxValue;
    
    @NotNull(message = "Payout amount is required")
    @DecimalMin(value = "0.0", message = "Payout amount must be non-negative")
    private BigDecimal payoutAmount;
} 