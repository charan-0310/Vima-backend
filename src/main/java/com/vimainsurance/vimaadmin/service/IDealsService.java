package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.DealsDashboardResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IDealsService {
ResponseEntity<ResponseDto<String>> createDeals(DealsRequestDto dealsRequestDto);
ResponseEntity<ResponseDto<String>> updateDeals(UUID individualId, DealsRequestDto dealsRequestDto);
ResponseEntity<ResponseDto<DealsResponseDto>> getDealsById(UUID individualId);
ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllDeals();
ResponseEntity<ResponseDto<String>> deleteDeals(UUID individualId);
ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(UUID individualId);
ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, UUID individualId);
ResponseEntity<ResponseDto<String>> uploadPolicyWithDetails(PolicyUploadRequestDto requestDto);
ResponseEntity<Resource> downloadDocument(String documentId);
ResponseEntity<ResponseDto<String>> deleteDocument(String documentId);
ResponseEntity<ResponseDto<DealsDashboardResponseDto>> getDashboardMetrics();
ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllWithFilters(String search, String status, String productType, int page, int rec, String sortBy, String sortDirection);
}
