package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.EndorsementRequestDto;
import com.vimainsurance.vimaadmin.dto.EndorsementResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEndorsementService {
    
    ResponseEntity<ResponseDto<String>> create(EndorsementRequestDto requestDto);
    
    ResponseEntity<ResponseDto<String>> update(EndorsementRequestDto requestDto);
    
    ResponseEntity<ResponseDto<String>> delete(UUID endorsementId);
    
    ResponseEntity<ResponseDto<EndorsementResponseDto>> getById(UUID endorsementId);
    
    ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getAll();
    
    ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByOrganizationId(UUID organizationId);
    
    ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByStatus(String status);
    
    ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByEndorsementType(String endorsementType);
    
    ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getAllWithFilters(
        UUID organizationId, 
        String organizationName,
        String status, 
        String endorsementType,
        String uploadedBy,
        String fromDate,
        String toDate,
        int page, 
        int size, 
        String sortBy, 
        String sortDirection
    );
    
    ResponseEntity<ResponseDto<String>> approve(MultipartFile[]  files,EndorsementRequestDto requestDto);
    
    ResponseEntity<ResponseDto<String>> reject(UUID endorsementId);

    ResponseEntity<ResponseDto<String>> getPendingCount();

    ResponseEntity<ResponseDto<String>> confirm(UUID endorsementId);

    ResponseEntity<ResponseDto<String>> confirmSchedule();

    ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(String endorsementId, int page, int rec);

    ResponseEntity<Resource> downloadDocument(UUID endorsementId,String documentId);

}

