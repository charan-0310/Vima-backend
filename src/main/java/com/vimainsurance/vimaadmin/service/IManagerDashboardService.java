package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.manager.ManagerClaimsPipelineBucketDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardSummaryDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerMonthlyEndorsementDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerPolicyExpirySegmentDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerTatSummaryDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerUpcomingRenewalDto;

public interface IManagerDashboardService {

    ManagerDashboardSummaryDto getSummary();

    List<ManagerPolicyExpirySegmentDto> getPolicyExpiryStatus();

    List<ManagerClaimsPipelineBucketDto> getClaimsPipeline();

    List<ManagerMonthlyEndorsementDto> getMonthlyEndorsements(int months);

    List<ManagerUpcomingRenewalDto> getUpcomingRenewals(int days);

    ManagerTatSummaryDto getTatSummary();
}
