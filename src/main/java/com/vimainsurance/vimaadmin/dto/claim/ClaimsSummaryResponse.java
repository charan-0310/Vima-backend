package com.vimainsurance.vimaadmin.dto.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dashboard summary metrics for GET /api/v1/admin/claims/summary. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimsSummaryResponse {
    private long totalClaims;
    private long pendingReview;
    private long withInsurer;
    private long queryRaised;
    private long settled;
    private long closed;
    /** Combined count for Settlement KPI: settled + closed. */
    private long settlementClosedTotal;
    private long rejected;
    private double avgProcessingDays;
}
