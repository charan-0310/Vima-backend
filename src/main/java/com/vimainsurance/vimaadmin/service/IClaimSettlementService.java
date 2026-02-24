package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDeductionDto;
import com.vimainsurance.vimaadmin.dto.claim.DeductionRequest;
import com.vimainsurance.vimaadmin.dto.claim.SettlementRequest;
import com.vimainsurance.vimaadmin.dto.claim.SettlementResponse;

public interface IClaimSettlementService {

    ResponseEntity<ResponseDto<SettlementResponse>> recordSettlement(UUID claimId, SettlementRequest request);

    /** Record settlement with optional document upload (multipart). */
    ResponseEntity<ResponseDto<SettlementResponse>> recordSettlementWithDocument(UUID claimId, SettlementRequest request, MultipartFile document);

    ResponseEntity<ResponseDto<SettlementResponse>> updateSettlement(UUID claimId, SettlementRequest request);

    /** Update settlement with optional document upload (multipart). */
    ResponseEntity<ResponseDto<SettlementResponse>> updateSettlementWithDocument(UUID claimId, SettlementRequest request, MultipartFile document);

    ResponseEntity<ResponseDto<ClaimDeductionDto>> addDeduction(UUID claimId, DeductionRequest request);

    ResponseEntity<ResponseDto<List<ClaimDeductionDto>>> listDeductions(UUID claimId);

    ResponseEntity<ResponseDto<Void>> removeDeduction(UUID claimId, UUID deductionId);
}
