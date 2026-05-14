package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardPortfolioDto {
    private int activeClients;
    private int clientDeltaFromLastMonth;
    private ManagerCoveredLivesDto coveredLives;
    private ManagerActivePoliciesDto activePolicies;
    private double totalAnnualPremium;
    private double premiumDeltaFromLastYear;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerCoveredLivesDto {
        private long employees;
        private long dependents;
        private long total;
        private int deltaFromLastMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerActivePoliciesDto {
        private long total;
        private long expiringIn30Days;
        private long expiringIn31To60Days;
    }
}
