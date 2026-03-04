package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.EmployeeUploadResponse;
import com.vimainsurance.vimaadmin.dto.ManualAddEmployeesRequestDto;
import com.vimainsurance.vimaadmin.dto.ManualDeleteEmployeesRequestDto;
import com.vimainsurance.vimaadmin.dto.BulkEmployeeDeletionRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
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
    ResponseEntity<Resource> downloadDocument(UUID organizationId, String documentId);
    ResponseEntity<ResponseDto<String>> deleteDocument(UUID organizationId, String documentId);
    ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(UUID organizationId);
    ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployees(UUID organizationId);
    ResponseEntity<ResponseDto<OrganizationEmployeeDto>> getEmployee(UUID individualId, UUID organizationId);
    ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployeeDependents(UUID individualId, UUID organizationId);
    ResponseEntity<ResponseDto<EmployeeUploadResponse>> uploadDealsFromCsv(MultipartFile file, UUID organizationId);
    ResponseEntity<ResponseDto<EmployeeUploadResponse>> deleteEmployeesFromCsv(MultipartFile file, UUID organizationId);
    ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.CsvValidationResponseDto>> validateCsv(MultipartFile file, UUID organizationId, String operation);
    ResponseEntity<ResponseDto<EmployeeUploadResponse>> delete(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList, UUID organizationId, String uploadType, MultipartFile file);
    ResponseEntity<ResponseDto<String>> deleteEmployee(String employeeId, UUID organizationId);
    ResponseEntity<ResponseDto<String>> bulkDeleteEmployees(com.vimainsurance.vimaadmin.dto.BulkEmployeeDeletionRequestDto requestDto, UUID organizationId);
    ResponseEntity<ResponseDto<EmployeeUploadResponse>> uploadEmployees(List<EmployeeUploadDto> employeeUploadDtoList, UUID organizationId, String uploadType, MultipartFile file);
    ResponseEntity<ResponseDto<EmployeeUploadResponse>> manualAddEmployees(UUID organizationId, ManualAddEmployeesRequestDto requestDto);
    ResponseEntity<ResponseDto<String>> manualDeleteEmployees(UUID organizationId, ManualDeleteEmployeesRequestDto requestDto);
    ResponseEntity<ResponseDto<EmployeeUploadResponse>> validateEmployees(List<EmployeeUploadDto> employeeUploadDtoList, UUID organizationId);
    ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployeesByEndorsementId(UUID endorsementId, int page, int rec);
}


