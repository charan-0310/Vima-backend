package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimQueryDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeResponseRequest;
import com.vimainsurance.vimaadmin.dto.claim.QueryCreateRequest;
import com.vimainsurance.vimaadmin.dto.claim.QueryCreateResponse;
import com.vimainsurance.vimaadmin.dto.claim.QueryResponseRequest;

public interface IClaimQueryService {

    ResponseEntity<ResponseDto<QueryCreateResponse>> createQuery(UUID claimId, QueryCreateRequest request);

    ResponseEntity<ResponseDto<Void>> respondToQuery(UUID claimId, UUID queryId, QueryResponseRequest request);

    ResponseEntity<ResponseDto<Void>> addEmployeeResponse(UUID claimId, UUID queryId, EmployeeResponseRequest request);

    ResponseEntity<ResponseDto<List<ClaimQueryDto>>> listQueries(UUID claimId);

    /** List queries for a claim, restricted to current employee's own claims. */
    ResponseEntity<ResponseDto<List<ClaimQueryDto>>> listQueriesForEmployee(UUID claimId);
}
