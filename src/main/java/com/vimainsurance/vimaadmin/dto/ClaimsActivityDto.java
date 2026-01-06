package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimsActivityDto {
    private List<MonthlyClaimsActivityDto> monthlyClaimsActivity;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyClaimsActivityDto {
        private String month; // Format: "MMM yyyy" (e.g., "Jan 2024")
        private Long claimsCount;
        private BigDecimal amount; // Amount in ₹ (Indian Rupees)
    }
}

