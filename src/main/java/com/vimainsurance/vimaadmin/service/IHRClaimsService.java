package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.HRClaimsSummaryResponse;
import com.vimainsurance.vimaadmin.dto.claim.MonthlyActivityResponse;

/**
 * HR Admin company-level claims dashboard: list, detail (read-only), summary, CSV export, monthly activity.
 * Uses all organization IDs from JWT (HR can see claims for every org they have access to).
 */
public interface IHRClaimsService {

    ResponseEntity<ResponseDto<Page<ClaimDetailsResponse>>> listCompanyClaims(
            List<UUID> organizationIds, ClaimListFilters filters, Pageable pageable);

    ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetail(UUID claimId, List<UUID> organizationIds);

    ResponseEntity<ResponseDto<HRClaimsSummaryResponse>> getCompanySummary(List<UUID> organizationIds);

    ResponseEntity<ResponseDto<MonthlyActivityResponse>> getMonthlyActivity(List<UUID> organizationIds, int months);

    ResponseEntity<StreamingResponseBody> exportClaimsCsv(List<UUID> organizationIds, ClaimListFilters filters, String filename);
}
