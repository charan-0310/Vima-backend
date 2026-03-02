package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSubmissionRequest;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSummaryDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeClaimSubmitResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeClaimsSummaryResponse;

/**
 * Employee-facing claims API: submit, list own claims, get details, update draft, cancel.
 * All operations are scoped to the authenticated employee (JWT employee_id / individual_id).
 */
public interface IEmployeeClaimsService {

    /**
     * Submit a new claim. Requires JWT with employee_id or individual_id.
     * Resolves organizationId from policy if not provided.
     */
    ResponseEntity<ResponseDto<EmployeeClaimSubmitResponseDto>> submitClaim(ClaimSubmissionRequest request);

    /**
     * List current employee's claims with optional filters. Paginated and sortable.
     */
    ResponseEntity<ResponseDto<Page<ClaimSummaryDto>>> getMyClaims(
            String status,
            String claimType,
            String dateFrom,
            String dateTo,
            Pageable pageable);

    /**
     * Get claim details. Returns 403 if the claim does not belong to the current employee.
     */
    ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetails(UUID claimId);

    /**
     * Update a DRAFT claim. Returns 403 if not owned by current employee.
     */
    ResponseEntity<ResponseDto<ClaimDetailsResponse>> updateDraftClaim(UUID claimId, ClaimSubmissionRequest request);

    /**
     * Cancel a DRAFT or PENDING_REVIEW claim. Returns 403 if not owned by current employee.
     */
    ResponseEntity<ResponseDto<String>> cancelClaim(UUID claimId);

    /**
     * Get summary for current employee's claims (total count, total paid from settled claims).
     */
    ResponseEntity<ResponseDto<EmployeeClaimsSummaryResponse>> getMyClaimsSummary();
}
