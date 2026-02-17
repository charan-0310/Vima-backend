package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.service.IHRClaimsService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

/**
 * HR Admin company-level claims dashboard: list, detail (read-only), summary, CSV export, monthly activity.
 * Base path: /api/v1/hr/claims. HR can only see claims for their own organization (from JWT).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/hr/claims")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
@RequiredArgsConstructor
public class HRClaimsController {

    private static final String DEFAULT_SORT = "dateOfSubmission";
    private static final String DEFAULT_DIRECTION = "desc";

    private final IHRClaimsService hrClaimsService;
    private final JwtUserExtractor jwtUserExtractor;

    private List<UUID> resolveHrOrganizationIds() {
        List<String> orgIds = jwtUserExtractor.getCurrentOrganizations();
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>();
        for (String s : orgIds) {
            try {
                result.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip invalid UUIDs
            }
        }
        return result;
    }

    /** GET /api/v1/hr/claims - List company claims (paginated, filtered). */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<ClaimDetailsResponse>>> listCompanyClaims(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String claimType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = DEFAULT_DIRECTION) String sortDirection) {
        List<UUID> orgIds = resolveHrOrganizationIds();
        if (orgIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("No organization context", null));
        }
        ClaimListFilters filters = buildFilters(orgIds, status, claimType, search, dateFrom, dateTo);
        String orderBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, orderBy));
        return hrClaimsService.listCompanyClaims(orgIds, filters, pageable);
    }

    /** GET /api/v1/hr/claims/summary - Company claims summary (KPI cards). */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        List<UUID> orgIds = resolveHrOrganizationIds();
        if (orgIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("No organization context", null));
        }
        return hrClaimsService.getCompanySummary(orgIds);
    }

    /** GET /api/v1/hr/claims/export - Export claims as CSV. */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String claimType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        List<UUID> orgIds = resolveHrOrganizationIds();
        if (orgIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ClaimListFilters filters = buildFilters(orgIds, status, claimType, search, dateFrom, dateTo);
        String filename = "claims_export_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
        return hrClaimsService.exportClaimsCsv(orgIds, filters, filename);
    }

    /** GET /api/v1/hr/claims/activity - Monthly claims activity (for chart). */
    @GetMapping("/activity")
    public ResponseEntity<?> getActivity(@RequestParam(defaultValue = "12") int months) {
        List<UUID> orgIds = resolveHrOrganizationIds();
        if (orgIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("No organization context", null));
        }
        if (months < 1 || months > 60) {
            months = 12;
        }
        return hrClaimsService.getMonthlyActivity(orgIds, months);
    }

    /** GET /api/v1/hr/claims/{claimId} - Get claim detail (read-only, own company only). */
    @GetMapping("/{claimId}")
    public ResponseEntity<ResponseDto<ClaimDetailsResponse>> getClaimDetail(@PathVariable UUID claimId) {
        List<UUID> orgIds = resolveHrOrganizationIds();
        if (orgIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>("No organization context", null));
        }
        return hrClaimsService.getClaimDetail(claimId, orgIds);
    }

    private ClaimListFilters buildFilters(List<UUID> organizationIds, String status, String claimType, String search,
                                         String dateFrom, String dateTo) {
        ClaimListFilters filters = new ClaimListFilters();
        if (organizationIds != null && !organizationIds.isEmpty()) {
            filters.setOrganizationIds(organizationIds);
        }
        if (status != null && !status.isBlank()) {
            try {
                filters.setInternalStatus(ClaimStatus.fromValue(status.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (claimType != null && !claimType.isBlank()) {
            try {
                filters.setClaimType(ClaimType.fromValue(claimType.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (search != null && !search.isBlank()) {
            filters.setSearch(search.trim());
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
        return filters;
    }
}
