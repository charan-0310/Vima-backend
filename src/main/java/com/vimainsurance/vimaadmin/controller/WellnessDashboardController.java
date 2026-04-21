package com.vimainsurance.vimaadmin.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessAccessLogResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessDashboardDto;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.service.IWellnessAccessService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/wellness")
@RequiredArgsConstructor
public class WellnessDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(WellnessDashboardController.class);

    private final IWellnessAccessService wellnessAccessService;
    private final JwtUserExtractor jwtUserExtractor;

    @GetMapping("/access/logs")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<Page<WellnessAccessLogResponseDto>>> getAccessLogs(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("[correlationId:{}] GET /api/v1/wellness/access/logs called", MDC.get("correlationId"));
        BaseResponse<Page<WellnessAccessLogResponseDto>> responseObj = new BaseResponse<>();
        UUID effectiveOrgId = resolveEffectiveOrgId(orgId);
        if (effectiveOrgId == null) {
            return responseObj.render(responseObj.formErrorResponse(403, "No organization context"));
        }
        return wellnessAccessService.getAccessLogs(effectiveOrgId, page, size);
    }

    @GetMapping("/dashboard/{orgId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<WellnessDashboardDto>> getDashboardStats(@PathVariable UUID orgId) {
        logger.info("[correlationId:{}] GET /api/v1/wellness/dashboard/{} called", MDC.get("correlationId"), orgId);
        BaseResponse<WellnessDashboardDto> responseObj = new BaseResponse<>();
        UUID effectiveOrgId = resolveEffectiveOrgId(orgId);
        if (effectiveOrgId == null) {
            return responseObj.render(responseObj.formErrorResponse(403, "No organization context"));
        }
        return wellnessAccessService.getDashboardStats(effectiveOrgId);
    }

    private UUID resolveEffectiveOrgId(UUID requestedOrgId) {
        UserRole currentRole = jwtUserExtractor.getCurrentUserRole();
        if (UserRole.HR_ADMIN.equals(currentRole)) {
            List<UUID> hrOrgs = resolveHrOrganizationIds();
            return hrOrgs.isEmpty() ? null : hrOrgs.get(0);
        }
        return requestedOrgId;
    }

    private List<UUID> resolveHrOrganizationIds() {
        List<String> orgIds = jwtUserExtractor.getCurrentOrganizations();
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>();
        for (String orgId : orgIds) {
            try {
                result.add(UUID.fromString(orgId));
            } catch (IllegalArgumentException ignored) {
                // Skip invalid org ids in token.
            }
        }
        return result;
    }
}
