package com.vimainsurance.vimaadmin.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDocumentListResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSubmissionRequest;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSummaryDto;
import com.vimainsurance.vimaadmin.dto.claim.DocumentUploadResponse;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeClaimSubmitResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeClaimsSummaryResponse;
import com.vimainsurance.vimaadmin.dto.claim.EmployeeResponseRequest;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.service.IClaimQueryService;
import com.vimainsurance.vimaadmin.service.IClaimsDocumentService;
import com.vimainsurance.vimaadmin.service.IEmployeeClaimsService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Employee-facing claims API: submit, list own claims, get details, update draft, cancel.
 * All operations are scoped to the authenticated employee (JWT employee_id / individual_id).
 * Base path: /api/v1/claims (with context path e.g. /dev/api/v1/claims).
 */
@RestController
@RequestMapping("/api/v1/claims")
@PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeClaimsController {

    private static final String DEFAULT_SORT = "dateOfSubmission";
    private static final String DEFAULT_DIRECTION = "desc";

    private final IEmployeeClaimsService employeeClaimsService;
    private final IClaimsDocumentService claimsDocumentService;
    private final IClaimQueryService claimQueryService;
    private final JwtUserExtractor jwtUserExtractor;

    /**
     * POST /api/v1/claims - Submit a new claim.
     * Also supports POST /api/v1/claims/submit for compatibility.
     */
    @PostMapping(value = { "", "/submit" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<EmployeeClaimSubmitResponseDto>> submitClaim(
            @Valid @RequestBody ClaimSubmissionRequest request) {
        return employeeClaimsService.submitClaim(request);
    }

    /**
     * GET /api/v1/claims/summary - Summary for current employee's claims (total count, total paid from settled).
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<EmployeeClaimsSummaryResponse>> getMyClaimsSummary() {
        return employeeClaimsService.getMyClaimsSummary();
    }

    /**
     * GET /api/v1/claims/my - List current employee's claims (paginated, sortable).
     * Filters: status, claimType, dateFrom, dateTo (ISO date).
     * Default: page=0, size=20, sort=dateOfSubmission,desc.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<Page<ClaimSummaryDto>>> getMyClaims(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String claimType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = DEFAULT_DIRECTION) String sortDirection) {
        String orderBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, orderBy));
        return employeeClaimsService.getMyClaims(status, claimType, dateFrom, dateTo, pageable);
    }

    /**
     * GET /api/v1/claims/{claimId} - Get claim details (own claims only). Returns 403 if not owned.
     */
    @GetMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetails(@PathVariable UUID claimId) {
        return employeeClaimsService.getClaimDetails(claimId);
    }

    /**
     * PUT /api/v1/claims/{claimId} - Update a DRAFT claim. Returns 403 if not owned or not draft.
     */
    @PutMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> updateDraftClaim(
            @PathVariable UUID claimId,
            @Valid @RequestBody ClaimSubmissionRequest request) {
        return employeeClaimsService.updateDraftClaim(claimId, request);
    }

    /**
     * DELETE /api/v1/claims/{claimId} - Cancel a DRAFT or PENDING_REVIEW claim. Returns 403 if not owned.
     */
    @DeleteMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<String>> cancelClaim(@PathVariable UUID claimId) {
        return employeeClaimsService.cancelClaim(claimId);
    }

    /**
     * GET /api/v1/claims/{claimId}/queries - List queries for this claim (employee's own claims only).
     */
    @GetMapping("/{claimId}/queries")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<java.util.List<com.vimainsurance.vimaadmin.dto.claim.ClaimQueryDto>>> getClaimQueries(@PathVariable UUID claimId) {
        return claimQueryService.listQueriesForEmployee(claimId);
    }

    /**
     * POST /api/v1/claims/{claimId}/queries/{queryId}/employee-response - Employee adds remarks/docs for query (no status change).
     */
    @PostMapping("/{claimId}/queries/{queryId}/employee-response")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<Void>> addEmployeeQueryResponse(
            @PathVariable UUID claimId,
            @PathVariable UUID queryId,
            @Valid @RequestBody EmployeeResponseRequest request) {
        return claimQueryService.addEmployeeResponse(claimId, queryId, request);
    }

    /**
     * POST /api/v1/claims/{claimId}/documents - Upload claim documents (multipart).
     * Params: files (multipart), documentType (enum: CLAIM_FORM, MEDICAL_BILL, etc.).
     */
    @PostMapping(value = "/{claimId}/documents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<DocumentUploadResponse>> uploadClaimDocuments(
            @PathVariable UUID claimId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("documentType") String documentType) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }
        DocumentType type;
        try {
            type = DocumentType.fromValue(documentType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Invalid documentType: " + documentType));
        }
        return claimsDocumentService.uploadClaimsDocuments(files, claimId, type, employeeId, "EMPLOYEE");
    }

    /**
     * GET /api/v1/claims/{claimId}/documents - List claim documents with pre-signed download URLs (own claims only).
     */
    @GetMapping("/{claimId}/documents")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ResponseDto<ClaimDocumentListResponse>> getClaimDocuments(@PathVariable UUID claimId) {
        UUID employeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (employeeId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Employee context not found in token."));
        }
        return claimsDocumentService.getClaimDocuments(claimId, employeeId, false);
    }
}
