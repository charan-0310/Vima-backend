package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeDto;
import com.vimainsurance.vimaadmin.dto.OrganizationRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IOrganizationService {
    ResponseEntity<ResponseDto<String>> create(OrganizationRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> update(OrganizationRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> delete(UUID organizationId);
    ResponseEntity<ResponseDto<OrganizationResponseDto>> getById(UUID organizationId);
    ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAll();
    ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAllActive();
    ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAllWithFilters(String search, String status, int page, int rec, String sortBy, String sortDirection);
    ResponseEntity<ResponseDto<String>> uploadLogo(MultipartFile file, UUID organizationId);
    ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, UUID organizationId);
    ResponseEntity<Resource> downloadDocument(String documentId);
    ResponseEntity<ResponseDto<String>> deleteDocument(String documentId);
    ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(UUID organizationId);
    ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployees(UUID organizationId);
    ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.CsvUploadResponseDto>> uploadDealsFromCsv(MultipartFile file, UUID organizationId);
}


