package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerClaimsPipelineBucketDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerDashboardSummaryDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerMonthlyEndorsementDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerPolicyExpirySegmentDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerTatSummaryDto;
import com.vimainsurance.vimaadmin.dto.manager.ManagerUpcomingRenewalDto;
import com.vimainsurance.vimaadmin.service.IManagerDashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Aggregated portfolio metrics for the manager dashboard (live data).
 * Base path: /api/v1/manager-dashboard
 */
@RestController
@RequestMapping("/api/v1/manager-dashboard")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final IManagerDashboardService managerDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ResponseDto<ManagerDashboardSummaryDto>> summary() {
        return ResponseEntity.ok(new ResponseDto<>("Success", managerDashboardService.getSummary()));
    }

    @GetMapping("/policy-expiry-status")
    public ResponseEntity<ResponseDto<List<ManagerPolicyExpirySegmentDto>>> policyExpiryStatus() {
        return ResponseEntity.ok(new ResponseDto<>("Success", managerDashboardService.getPolicyExpiryStatus()));
    }

    @GetMapping("/claims-pipeline")
    public ResponseEntity<ResponseDto<List<ManagerClaimsPipelineBucketDto>>> claimsPipeline() {
        return ResponseEntity.ok(new ResponseDto<>("Success", managerDashboardService.getClaimsPipeline()));
    }

    @GetMapping("/monthly-endorsements")
    public ResponseEntity<ResponseDto<List<ManagerMonthlyEndorsementDto>>> monthlyEndorsements(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(new ResponseDto<>("Success", managerDashboardService.getMonthlyEndorsements(months)));
    }

    @GetMapping("/upcoming-renewals")
    public ResponseEntity<ResponseDto<List<ManagerUpcomingRenewalDto>>> upcomingRenewals(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(new ResponseDto<>("Success", managerDashboardService.getUpcomingRenewals(days)));
    }

    @GetMapping("/tat-summary")
    public ResponseEntity<ResponseDto<ManagerTatSummaryDto>> tatSummary() {
        return ResponseEntity.ok(new ResponseDto<>("Success", managerDashboardService.getTatSummary()));
    }
}
