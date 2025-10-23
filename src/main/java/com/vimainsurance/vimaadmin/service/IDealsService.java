package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import java.util.UUID;
import java.util.List;

public interface IDealsService {
ResponseEntity<ResponseDto<String>> createDeals(DealsRequestDto dealsRequestDto);
ResponseEntity<ResponseDto<String>> updateDeals(UUID individualId, DealsRequestDto dealsRequestDto);
ResponseEntity<ResponseDto<DealsResponseDto>> getDealsById(UUID individualId);
ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllDeals();
ResponseEntity<ResponseDto<String>> deleteDeals(UUID individualId);
ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(UUID individualId);
ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, UUID individualId);
ResponseEntity<Resource> downloadDocument(String documentId);
ResponseEntity<ResponseDto<String>> deleteDocument(String documentId);
}
