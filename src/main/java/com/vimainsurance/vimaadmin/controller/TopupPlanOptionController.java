package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

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

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionRequestDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupOptionsResponseDto;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.service.ITopupPlanOptionService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class TopupPlanOptionController {

    private static final String TOPUP_FLAG = "enrollment.topup-plans";

    private final ITopupPlanOptionService topupPlanOptionService;
    private final FeatureFlagService featureFlagService;

    public TopupPlanOptionController(ITopupPlanOptionService topupPlanOptionService, FeatureFlagService featureFlagService) {
        this.topupPlanOptionService = topupPlanOptionService;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping("admin/companies/{companyId}/topup-options")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<List<TopupPlanOptionResponseDto>>> list(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String planType) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(TOPUP_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<List<TopupPlanOptionResponseDto>>().formErrorResponse(403, "Feature enrollment.topup-plans is not enabled"));
        }
        return topupPlanOptionService.listByCompany(companyId, planType);
    }

    @GetMapping("admin/companies/{companyId}/topup-options/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> getById(@PathVariable UUID id) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(TOPUP_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<TopupPlanOptionResponseDto>().formErrorResponse(403, "Feature enrollment.topup-plans is not enabled"));
        }
        return topupPlanOptionService.getById(id);
    }

    @PostMapping("admin/companies/{companyId}/topup-options")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> create(
            @PathVariable UUID companyId,
            @RequestBody @Valid TopupPlanOptionRequestDto dto) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(TOPUP_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<TopupPlanOptionResponseDto>().formErrorResponse(403, "Feature enrollment.topup-plans is not enabled"));
        }
        dto.setCompanyId(companyId);
        return topupPlanOptionService.create(dto);
    }

    @PutMapping("admin/companies/{companyId}/topup-options/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> update(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            @RequestBody @Valid TopupPlanOptionRequestDto dto) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(TOPUP_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<TopupPlanOptionResponseDto>().formErrorResponse(403, "Feature enrollment.topup-plans is not enabled"));
        }
        return topupPlanOptionService.update(id, companyId, dto);
    }

    @DeleteMapping("admin/companies/{companyId}/topup-options/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>> delete(
            @PathVariable UUID companyId,
            @PathVariable UUID id) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(TOPUP_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<String>().formErrorResponse(403, "Feature enrollment.topup-plans is not enabled"));
        }
        return topupPlanOptionService.softDelete(id, companyId);
    }

    /**
     * Employee: get active top-up options with premium preview (token-based, no JWT).
     */
    @GetMapping("enrollment/{token}/topup-options")
    public ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getOptionsWithPreview(@PathVariable String token) {
        return topupPlanOptionService.getActiveOptionsWithPremiumPreview(token);
    }
}
