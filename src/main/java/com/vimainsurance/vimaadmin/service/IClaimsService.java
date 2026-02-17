package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSubmissionRequest;
import com.vimainsurance.vimaadmin.dto.claim.InsurerRefRequest;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateRequest;

public interface IClaimsService {

    ClaimDetailsResponse submitClaim(ClaimSubmissionRequest request, UUID employeeId);

    ClaimDetailsResponse updateDraftClaim(UUID claimId, ClaimSubmissionRequest request, UUID employeeId);

    void cancelClaim(UUID claimId, UUID employeeId);

    ClaimDetailsResponse submitToInsurer(UUID claimId);

    ClaimDetailsResponse updateStatus(UUID claimId, StatusUpdateRequest request, UUID actorId, String actorRole);

    ClaimDetailsResponse updateInsurerRef(UUID claimId, InsurerRefRequest request);

    ClaimDetailsResponse getClaimDetails(UUID claimId);

    Page<ClaimDetailsResponse> listClaims(ClaimListFilters filters, Pageable pageable);
}
