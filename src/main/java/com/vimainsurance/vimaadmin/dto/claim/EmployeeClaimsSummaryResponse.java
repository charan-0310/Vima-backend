package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Employee claims summary for GET /api/v1/claims/summary (KPI cards on /employee/claims). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeClaimsSummaryResponse {
    private long totalClaims;
    /** Sum of settlement amountPaid for claims with status SETTLED. */
    private BigDecimal totalPaid;
    /** Optional: sum of claim amounts for in-progress claims (e.g. pending). */
    private BigDecimal totalPending;
}
