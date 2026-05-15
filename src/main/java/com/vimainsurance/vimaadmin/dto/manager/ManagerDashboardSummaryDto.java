package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardSummaryDto {
    private ManagerDashboardPortfolioDto portfolio;
    private ManagerDashboardAlertsDto alerts;
}
