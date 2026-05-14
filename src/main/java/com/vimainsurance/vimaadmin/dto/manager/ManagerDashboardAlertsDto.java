package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardAlertsDto {
    private long renewalsIn30Days;
    private long pendingEndorsements;
    private long claimsNeedingAction;
    private long enrollmentsExpiringSoon;
}
