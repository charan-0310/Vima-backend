package com.vimainsurance.vimaadmin.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.EndorsementRequestDto;
import com.vimainsurance.vimaadmin.dto.EndorsementResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeDto;
import com.vimainsurance.vimaadmin.dto.EmployeeOnboardingRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeeOnboardingResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IEndorsementService;
import com.vimainsurance.vimaadmin.service.IOrganizationService;

/**
 * Controller for Endorsement operations
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/batches")
public class EndorsementController {

    private static final Logger logger = LoggerFactory.getLogger(EndorsementController.class);

    @Autowired
    private IEndorsementService endorsementService;

    @Autowired
    private IOrganizationService organizationService;

    /**
     * Create a new endorsement
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> create(@RequestBody EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] /endorsements (POST) endpoint called", MDC.get("correlationId"));
        return endorsementService.create(requestDto);
    }

    /**
     * Update an existing endorsement
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> update(@RequestBody EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] /endorsements (PUT) endpoint called", MDC.get("correlationId"));
        return endorsementService.update(requestDto);
    }

    /**
     * Delete an endorsement
     */
    @DeleteMapping("/{endorsementId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> delete(@PathVariable UUID endorsementId) {
        logger.info("[correlationId:{}] /endorsements/{} (DELETE) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.delete(endorsementId);
    }

    /**
     * Get endorsement by ID
     */
    @GetMapping("/{endorsementId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<EndorsementResponseDto>> getById(@PathVariable UUID endorsementId) {
        logger.info("[correlationId:{}] /endorsements/{} (GET) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.getById(endorsementId);
    }

    /**
     * Get all endorsements
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getAll() {
        logger.info("[correlationId:{}] /endorsements (GET) endpoint called", MDC.get("correlationId"));
        return endorsementService.getAll();
    }

    /**
     * Get endorsements by organization ID
     */
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByOrganizationId(
            @PathVariable UUID organizationId) {
        logger.info("[correlationId:{}] /endorsements/organization/{} (GET) endpoint called", MDC.get("correlationId"), organizationId);
        return endorsementService.getByOrganizationId(organizationId);
    }

    /**
     * Get endorsements by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByStatus(@PathVariable String status) {
        logger.info("[correlationId:{}] /endorsements/status/{} (GET) endpoint called", MDC.get("correlationId"), status);
        return endorsementService.getByStatus(status);
    }

    /**
     * Get endorsements by endorsement type (ADDITION or DELETION)
     */
    @GetMapping("/type/{endorsementType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByEndorsementType(
            @PathVariable String endorsementType) {
        logger.info("[correlationId:{}] /endorsements/type/{} (GET) endpoint called", MDC.get("correlationId"), endorsementType);
        return endorsementService.getByEndorsementType(endorsementType);
    }

    /**
     * Get endorsements with filters, pagination, and sorting
     * 
     * Query Parameters:
     * - organizationId: Filter by organization ID (optional)
     * - organizationName: Search by organization name (case-insensitive, partial match) (optional)
     * - status: Filter by status (optional)
     * - endorsementType: Filter by endorsement type - ADDITION or DELETION (optional)
     * - page: Page number (default: 0)
     * - size: Records per page (default: 10)
     * - sortBy: Field to sort by (default: createdAt)
     * - sortDirection: Sort direction - asc or desc (default: desc)
     */
    @GetMapping("/filtered")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getAllWithFilters(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String organizationName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String endorsementType,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortDirection) {
        logger.info("[correlationId:{}] /endorsements/filtered (GET) endpoint called", MDC.get("correlationId"));
        return endorsementService.getAllWithFilters(organizationId, organizationName, status, endorsementType, uploadedBy, fromDate, toDate, page, size, sortBy, sortDirection);
    }

    /**
     * Approve an endorsement
     */
    @PostMapping(value = "/approve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> approve(@RequestPart(value = "files", required = false) MultipartFile[] files, @RequestPart("requestDto") EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] /endorsements/approve (POST) endpoint called", MDC.get("correlationId"));
        return endorsementService.approve(files, requestDto);
    }

    /**
     * Reject an endorsement
     */
    @PostMapping("/{endorsementId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> reject(@PathVariable UUID endorsementId) {
        logger.info("[correlationId:{}] /endorsements/{}/reject (POST) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.reject(endorsementId);
    }

    /* 
     * Get pending count
     */
    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<String>> getPendingCount() {
        logger.info("[correlationId:{}] /endorsements/pending-count (GET) endpoint called", MDC.get("correlationId"));
        return endorsementService.getPendingCount();
    }

    /* 
    * Get employees by endorsement id with pagination
     */
    @GetMapping("/{endorsementId}/employees")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployeesByEndorsementId(@PathVariable UUID endorsementId, @RequestParam (defaultValue = "-1", required = false) int page, @RequestParam (defaultValue = "-1", required = false) int rec) {
        logger.info("[correlationId:{}] /endorsements/{}/employees (GET) endpoint called", MDC.get("correlationId"), endorsementId);
        return organizationService.getEmployeesByEndorsementId(endorsementId, page, rec);
    }


    /* 
    * Confirm an endorsement
    */
    @PostMapping("/{endorsementId}/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> confirm(@PathVariable UUID endorsementId) {
        logger.info("[correlationId:{}] /endorsements/{}/confirm (POST) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.confirm(endorsementId);
    }

    /* 
    * Get documents by endorsement id
    */
    @GetMapping("/{endorsementId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(@PathVariable UUID endorsementId, @RequestParam (defaultValue = "-1", required = false) int page, @RequestParam (defaultValue = "-1", required = false) int rec) {
        logger.info("[correlationId:{}] /endorsements/{}/documents (GET) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.getDocuments(endorsementId.toString(), page, rec);
    }
    
    /* 
    * Download document by endorsement id and document id
    */
    @GetMapping("/{endorsementId}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID endorsementId, @PathVariable String documentId) {
        logger.info("[correlationId:{}] /endorsements/{}/documents/{}/download (GET) endpoint called", MDC.get("correlationId"), endorsementId, documentId);
        return endorsementService.downloadDocument(endorsementId, documentId);
    }

    /* 
    * Employee onboarding by endorsement id (backward compatible)
    */
    @PostMapping("/{endorsementId}/employee-onboarding")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<EmployeeOnboardingResponseDto>> employeeOnboarding(@PathVariable UUID endorsementId) {
        logger.info("[correlationId:{}] /endorsements/{}/employee-onboarding (POST) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.employeeOnboarding(endorsementId);
    }

    /* 
    * Employee onboarding by endorsement id or individual ids
    */
    @PostMapping("/employee-onboarding")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<EmployeeOnboardingResponseDto>> employeeOnboarding(@RequestBody EmployeeOnboardingRequestDto requestDto) {
        logger.info("[correlationId:{}] /endorsements/employee-onboarding (POST) endpoint called with endorsementId: {}, individualIds: {}", 
            MDC.get("correlationId"), requestDto.getEndorsementId(), requestDto.getIndividualIds());
        return endorsementService.employeeOnboarding(requestDto);
    }

    @PostMapping("/delete/{endorsementId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> deactivateEndorsement(@PathVariable UUID endorsementId) {
        logger.info("[correlationId:{}] /endorsements/delete/{}/confirm-schedule (POST) endpoint called", MDC.get("correlationId"), endorsementId);
        return endorsementService.deactivateEndorsement(endorsementId);
    }
}
