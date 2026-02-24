package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.DeductionRequest;
import com.vimainsurance.vimaadmin.dto.claim.DocumentUploadResponse;
import com.vimainsurance.vimaadmin.dto.claim.InsurerRefRequest;
import com.vimainsurance.vimaadmin.dto.claim.QueryCreateRequest;
import com.vimainsurance.vimaadmin.dto.claim.QueryResponseRequest;
import com.vimainsurance.vimaadmin.dto.claim.SettlementRequest;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateRequest;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.service.IAdminClaimsService;
import com.vimainsurance.vimaadmin.service.IClaimQueryService;
import com.vimainsurance.vimaadmin.service.IClaimSettlementService;
import com.vimainsurance.vimaadmin.service.IClaimsDocumentService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * VIMA Admin claims API: list, detail, status, insurer-ref, submit-to-insurer, audit-log, summary, documents.
 * Base path: /api/v1/admin/claims
 */
@RestController
@RequestMapping("/api/v1/admin/claims")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
@RequiredArgsConstructor
public class AdminClaimsController {

    private static final String DEFAULT_SORT = "dateOfSubmission";
    private static final String DEFAULT_DIRECTION = "desc";

    private final IAdminClaimsService adminClaimsService;
    private final IClaimsDocumentService claimsDocumentService;
    private final IClaimQueryService claimQueryService;
    private final IClaimSettlementService claimSettlementService;
    private final JwtUserExtractor jwtUserExtractor;

    /** GET /api/v1/admin/claims - List all claims with filters. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<Page<ClaimDetailsResponse>>> listClaims(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String claimType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) UUID insurerId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = DEFAULT_DIRECTION) String sortDirection) {
        ClaimListFilters filters = new ClaimListFilters();
        filters.setOrganizationId(organizationId);
        filters.setIsDeleted(false);
        if (status != null && !status.isBlank()) {
            String statusVal = status.trim();
            if ("REJECTED".equalsIgnoreCase(statusVal)) {
                filters.setInternalStatusIn(Arrays.asList(
                        ClaimStatus.REJECTED,
                        ClaimStatus.REJECTED_BY_ADMIN,
                        ClaimStatus.INTIMATION_REJECTED));
            } else {
                try {
                    filters.setInternalStatus(ClaimStatus.fromValue(statusVal));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (claimType != null && !claimType.isBlank()) {
            try {
                filters.setClaimType(ClaimType.fromValue(claimType.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            try {
                filters.setDateOfSubmissionFrom(LocalDate.parse(dateFrom.trim()));
            } catch (Exception ignored) {}
        }
        if (dateTo != null && !dateTo.isBlank()) {
            try {
                filters.setDateOfSubmissionTo(LocalDate.parse(dateTo.trim()));
            } catch (Exception ignored) {}
        }
        filters.setInsurerId(insurerId);
        filters.setSearch(search != null && search.isBlank() ? null : search);
        String orderBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, orderBy));
        return adminClaimsService.listClaims(filters, pageable);
    }

    /** GET /api/v1/admin/claims/summary - Dashboard summary metrics. */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.claim.ClaimsSummaryResponse>> getSummary(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        ClaimListFilters filters = new ClaimListFilters();
        filters.setOrganizationId(organizationId);
        filters.setIsDeleted(false);
        if (dateFrom != null && !dateFrom.isBlank()) {
            try {
                filters.setDateOfSubmissionFrom(LocalDate.parse(dateFrom.trim()));
            } catch (Exception ignored) {}
        }
        if (dateTo != null && !dateTo.isBlank()) {
            try {
                filters.setDateOfSubmissionTo(LocalDate.parse(dateTo.trim()));
            } catch (Exception ignored) {}
        }
        return adminClaimsService.getSummaryMetrics(filters);
    }

    /** GET /api/v1/admin/claims/{claimId} - Get full claim details (admin view). */
    @GetMapping("/{claimId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetail(@PathVariable UUID claimId) {
        return adminClaimsService.getClaimDetail(claimId);
    }

    /** PUT /api/v1/admin/claims/{claimId}/status - Change claim status. */
    @PutMapping("/{claimId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.claim.StatusUpdateResponse>> changeStatus(
            @PathVariable UUID claimId,
            @Valid @RequestBody StatusUpdateRequest request) {
        return adminClaimsService.changeStatus(claimId, request);
    }

    /** PUT /api/v1/admin/claims/{claimId}/insurer-ref - Update insurer reference fields. */
    @PutMapping("/{claimId}/insurer-ref")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> updateInsurerRef(
            @PathVariable UUID claimId,
            @RequestBody InsurerRefRequest request) {
        return adminClaimsService.updateInsurerRef(claimId, request);
    }

    /** POST /api/v1/admin/claims/{claimId}/submit-to-insurer - Mark as submitted (calls adapter). */
    @PostMapping("/{claimId}/submit-to-insurer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> submitToInsurer(@PathVariable UUID claimId) {
        return adminClaimsService.submitToInsurer(claimId);
    }

    /** GET /api/v1/admin/claims/{claimId}/audit-log - View audit trail. */
    @GetMapping("/{claimId}/audit-log")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<java.util.List<ClaimDetailsResponse.ClaimAuditLogDto>>> getAuditLog(@PathVariable UUID claimId) {
        return adminClaimsService.getAuditLog(claimId);
    }

    /** POST /api/v1/admin/claims/{claimId}/queries - Create query record (claim status -> QUERY_RAISED). */
    @PostMapping("/{claimId}/queries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.claim.QueryCreateResponse>> createQuery(
            @PathVariable UUID claimId,
            @Valid @RequestBody QueryCreateRequest request) {
        return claimQueryService.createQuery(claimId, request);
    }

    /** PUT /api/v1/admin/claims/{claimId}/queries/{queryId}/respond - Record query response (claim status -> QUERY_RESPONDED). */
    @PutMapping("/{claimId}/queries/{queryId}/respond")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> respondToQuery(
            @PathVariable UUID claimId,
            @PathVariable UUID queryId,
            @Valid @RequestBody QueryResponseRequest request) {
        return claimQueryService.respondToQuery(claimId, queryId, request);
    }

    /** GET /api/v1/admin/claims/{claimId}/queries - List all queries for claim. */
    @GetMapping("/{claimId}/queries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<java.util.List<com.vimainsurance.vimaadmin.dto.claim.ClaimQueryDto>>> listQueries(@PathVariable UUID claimId) {
        return claimQueryService.listQueries(claimId);
    }

    /** POST /api/v1/admin/claims/{claimId}/settlement - Record settlement (claim status -> SETTLED). */
    @PostMapping("/{claimId}/settlement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.claim.SettlementResponse>> recordSettlement(
            @PathVariable UUID claimId,
            @Valid @RequestBody SettlementRequest request) {
        return claimSettlementService.recordSettlement(claimId, request);
    }

    /** PUT /api/v1/admin/claims/{claimId}/settlement - Update settlement. */
    @PutMapping("/{claimId}/settlement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.claim.SettlementResponse>> updateSettlement(
            @PathVariable UUID claimId,
            @Valid @RequestBody SettlementRequest request) {
        return claimSettlementService.updateSettlement(claimId, request);
    }

    /** POST /api/v1/admin/claims/{claimId}/deductions - Add deduction line item. */
    @PostMapping("/{claimId}/deductions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.claim.ClaimDeductionDto>> addDeduction(
            @PathVariable UUID claimId,
            @Valid @RequestBody DeductionRequest request) {
        return claimSettlementService.addDeduction(claimId, request);
    }

    /** GET /api/v1/admin/claims/{claimId}/deductions - List deductions. */
    @GetMapping("/{claimId}/deductions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<java.util.List<com.vimainsurance.vimaadmin.dto.claim.ClaimDeductionDto>>> listDeductions(@PathVariable UUID claimId) {
        return claimSettlementService.listDeductions(claimId);
    }

    /** DELETE /api/v1/admin/claims/{claimId}/deductions/{deductionId} - Remove deduction. */
    @DeleteMapping("/{claimId}/deductions/{deductionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> removeDeduction(
            @PathVariable UUID claimId,
            @PathVariable UUID deductionId) {
        return claimSettlementService.removeDeduction(claimId, deductionId);
    }

    /**
     * POST /api/v1/admin/claims/{claimId}/documents - Upload admin documents (claim letters, etc.).
     * Params: files (multipart), documentType (enum).
     */
    @PostMapping(value = "/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<DocumentUploadResponse>> uploadClaimDocuments(
            @PathVariable UUID claimId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("documentType") String documentType) {
        UUID adminId = jwtUserExtractor.getCurrentUserId();
        UserRole role = jwtUserExtractor.getCurrentUserRole();
        if (adminId == null || role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Admin context not found in token."));
        }
        DocumentType type;
        try {
            type = DocumentType.fromValue(documentType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Invalid documentType: " + documentType));
        }
        return claimsDocumentService.uploadClaimsDocuments(files, claimId, type, adminId, role.getValue());
    }

    /**
     * GET /api/v1/admin/claims/{claimId}/documents - List all claim documents with pre-signed URLs.
     */
    @GetMapping("/{claimId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<ClaimDocumentListResponse>> getClaimDocuments(@PathVariable UUID claimId) {
        UUID adminId = jwtUserExtractor.getCurrentUserId();
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Admin context not found in token."));
        }
        return claimsDocumentService.getClaimDocuments(claimId, adminId, true);
    }

    /**
     * DELETE /api/v1/admin/claims/{claimId}/documents/{docId} - Remove document.
     */
    @DeleteMapping("/{claimId}/documents/{docId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteClaimDocument(
            @PathVariable UUID claimId,
            @PathVariable UUID docId) {
        UUID adminId = jwtUserExtractor.getCurrentUserId();
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(401, "Authentication required. Admin context not found in token."));
        }
        return claimsDocumentService.deleteDocument(claimId, docId, adminId);
    }
}
