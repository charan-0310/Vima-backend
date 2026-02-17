package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.ClaimsSummaryResponse;
import com.vimainsurance.vimaadmin.dto.claim.InsurerRefRequest;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateRequest;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateResponse;

/**
 * VIMA Admin claims management: list, detail, status change, insurer ref, submit to insurer, audit log, summary.
 */
public interface IAdminClaimsService {

    ResponseEntity<ResponseDto<Page<ClaimDetailsResponse>>> listClaims(ClaimListFilters filters, Pageable pageable);

    ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetail(UUID claimId);

    ResponseEntity<ResponseDto<StatusUpdateResponse>> changeStatus(UUID claimId, StatusUpdateRequest request);

    ResponseEntity<ResponseDto<ClaimDetailsResponse>> updateInsurerRef(UUID claimId, InsurerRefRequest request);

    ResponseEntity<ResponseDto<ClaimDetailsResponse>> submitToInsurer(UUID claimId);

    ResponseEntity<ResponseDto<List<ClaimDetailsResponse.ClaimAuditLogDto>>> getAuditLog(UUID claimId);

    ResponseEntity<ResponseDto<ClaimsSummaryResponse>> getSummaryMetrics(ClaimListFilters filters);
}
