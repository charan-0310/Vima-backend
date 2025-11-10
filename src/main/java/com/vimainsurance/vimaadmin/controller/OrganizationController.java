package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.CsvUploadResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeDto;
import com.vimainsurance.vimaadmin.dto.OrganizationRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IOrganizationService;
import org.springframework.http.MediaType;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class OrganizationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

    @Autowired
    private IOrganizationService organizationService;

    @PostMapping("/organization")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> create(@RequestBody OrganizationRequestDto requestDto) {
        logger.info("[correlationId:{}] /organization (POST) endpoint called", MDC.get("correlationId"));
        return organizationService.create(requestDto);
    }

    @PutMapping("/organization")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> update(@RequestBody OrganizationRequestDto requestDto) {
        logger.info("[correlationId:{}] /organization (PUT) endpoint called", MDC.get("correlationId"));
        return organizationService.update(requestDto);
    }

    @DeleteMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> delete(@PathVariable UUID organizationId) {
        logger.info("[correlationId:{}] /organization/{} (DELETE) endpoint called", MDC.get("correlationId"), organizationId);
        return organizationService.delete(organizationId);
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<OrganizationResponseDto>> getById(@PathVariable UUID organizationId) {
        logger.info("[correlationId:{}] /organization/{} (GET) endpoint called", MDC.get("correlationId"), organizationId);
        return organizationService.getById(organizationId);
    }

    @GetMapping("/organizations")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAll() {
        logger.info("[correlationId:{}] /organizations (GET) endpoint called", MDC.get("correlationId"));
        return organizationService.getAll();
    }

    @GetMapping("/organizations/active")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAllActive() {
        logger.info("[correlationId:{}] /organizations/active (GET) endpoint called", MDC.get("correlationId"));
        return organizationService.getAllActive();
    }

    /**
     * Get organizations with pagination, search, filtering, and sorting
     * 
     * Query Parameters:
     * - page: Page number (default: 0)
     * - rec: Records per page (default: 10)
     * - search: Search term (searches in name, GSTIN, PAN, email, phone)
     * - status: Filter by status (ACTIVE/INACTIVE)
     * - sortBy: Field to sort by (organizationName, gstin, panNumber, email, phone, status, createdAt, updatedAt)
     * - sortDirection: Sort direction (asc/desc, default: desc)
     * 
     * Special: Use page=-1 and rec=-1 to get all records without pagination
     */
    @GetMapping("/organizations/filtered")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAllWithFilters(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int rec,
            @RequestParam(defaultValue = "", required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortDirection) {
        logger.info("[correlationId:{}] /organizations/filtered (GET) endpoint called", MDC.get("correlationId"));
        return organizationService.getAllWithFilters(search, status, page, rec, sortBy, sortDirection);
    }

    @GetMapping("/organization/{organizationId}/documents")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(@PathVariable UUID organizationId) {
        logger.info("[correlationId:{}] /organization/{}/documents (GET) endpoint called", MDC.get("correlationId"), organizationId);
        return organizationService.getDocuments(organizationId);
    }

    @PostMapping("/organization/{organizationId}/documents/upload")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> uploadDocument(
            @PathVariable UUID organizationId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("documentType") String documentType,
            @RequestParam("documentCategory") String documentCategory,
            @RequestParam(value = "notes", required = false) String notes) {
        logger.info("[correlationId:{}] /organization/{}/documents/upload (POST) endpoint called", MDC.get("correlationId"), organizationId);
        DocumentRequestDto requestDto = new DocumentRequestDto();
        requestDto.setFiles(files);
        requestDto.setDocumentType(documentType);
        requestDto.setDocumentCategory(documentCategory);
        requestDto.setNotes(notes);
        return organizationService.uploadDocument(requestDto, organizationId);
    }

    @GetMapping("/organization/documents/{documentId}/download")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {
        logger.info("[correlationId:{}] /organization/documents/{}/download (GET) endpoint called", MDC.get("correlationId"), documentId);
        return organizationService.downloadDocument(documentId);
    }

    @DeleteMapping("/organization/{organizationId}/documents/{documentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteDocument(@PathVariable UUID organizationId, @PathVariable String documentId) {
        logger.info("[correlationId:{}] /organization/{}/documents/{} (DELETE) endpoint called", MDC.get("correlationId"), organizationId, documentId);
        return organizationService.deleteDocument(documentId);
    }

    @GetMapping("/organization/{organizationId}/employees")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployees(@PathVariable UUID organizationId) {
        logger.info("[correlationId:{}] /organization/{}/employees (GET) endpoint called", MDC.get("correlationId"), organizationId);
        return organizationService.getEmployees(organizationId);
    }

    @PostMapping(value = "/organization/{organizationId}/upload/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<CsvUploadResponseDto>> uploadDealsFromCsv(
            @PathVariable UUID organizationId,
            @RequestParam("file") MultipartFile file) {
        logger.info("[correlationId:{}] /organization/{}/upload/csv endpoint called", MDC.get("correlationId"), organizationId);
        return organizationService.uploadDealsFromCsv(file, organizationId);
    }
}


