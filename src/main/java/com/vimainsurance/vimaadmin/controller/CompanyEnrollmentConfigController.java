package com.vimainsurance.vimaadmin.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigRequestDto;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.service.ICompanyEnrollmentConfigService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/companies/{companyId}/enrollment-config")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
public class CompanyEnrollmentConfigController {

    private static final String PARENT_COVERAGE_FLAG = "enrollment.parent-coverage";

    private final ICompanyEnrollmentConfigService configService;
    private final FeatureFlagService featureFlagService;

    public CompanyEnrollmentConfigController(ICompanyEnrollmentConfigService configService, FeatureFlagService featureFlagService) {
        this.configService = configService;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> get(@PathVariable UUID companyId) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(PARENT_COVERAGE_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<CompanyEnrollmentConfigResponseDto>().formErrorResponse(403, "Feature enrollment.parent-coverage is not enabled"));
        }
        return configService.getByCompanyId(companyId);
    }

    @PutMapping
    public ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> createOrUpdate(
            @PathVariable UUID companyId,
            @RequestBody CompanyEnrollmentConfigRequestDto dto) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(PARENT_COVERAGE_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<CompanyEnrollmentConfigResponseDto>().formErrorResponse(403, "Feature enrollment.parent-coverage is not enabled"));
        }
        dto.setOrganizationId(companyId);
        return configService.createOrUpdate(companyId, dto);
    }
}
