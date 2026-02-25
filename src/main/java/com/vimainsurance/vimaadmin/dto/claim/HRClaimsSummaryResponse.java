package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Company claims summary for GET /api/v1/hr/claims/summary (KPI cards). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HRClaimsSummaryResponse {
    private long totalClaims;
    private long pendingReview;
    private long withInsurer;
    private long settled;
    private long rejected;
    private int avgTatDays;
    private BigDecimal totalClaimAmount;
    private BigDecimal totalSettledAmount;
}
