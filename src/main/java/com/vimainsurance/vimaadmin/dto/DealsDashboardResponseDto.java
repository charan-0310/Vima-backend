package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for dashboard metrics response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DealsDashboardResponseDto {
    
    private Long totalCustomers;
    private BigDecimal totalCoverage;
    private Long totalActivePolicies;
    private BigDecimal totalPremium;
    
}
