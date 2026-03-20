package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
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
import com.vimainsurance.vimaadmin.dto.CostSharingRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.CostSharingRuleResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.service.ICostSharingRuleService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/companies/{companyId}/cost-sharing-rules")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
public class CostSharingRuleController {

    private static final String COST_SHARING_FLAG = "enrollment.cost-sharing";

    private final ICostSharingRuleService costSharingRuleService;
    private final FeatureFlagService featureFlagService;
    private final BaseResponse<List<CostSharingRuleResponseDto>> baseResponse = new BaseResponse<>();

    public CostSharingRuleController(ICostSharingRuleService costSharingRuleService, FeatureFlagService featureFlagService) {
        this.costSharingRuleService = costSharingRuleService;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<CostSharingRuleResponseDto>>> list(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String planType,
            @RequestParam(required = false) LocalDate effectiveDate) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(COST_SHARING_FLAG)) {
            return ResponseEntity.status(403).body(baseResponse.formErrorResponse(403, "Feature enrollment.cost-sharing is not enabled"));
        }
        return costSharingRuleService.listByCompany(companyId, planType, effectiveDate);
    }

    @PostMapping
    public ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> create(
            @PathVariable UUID companyId,
            @RequestBody @Valid CostSharingRuleRequestDto dto) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(COST_SHARING_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<CostSharingRuleResponseDto>().formErrorResponse(403, "Feature enrollment.cost-sharing is not enabled"));
        }
        dto.setCompanyId(companyId);
        return costSharingRuleService.create(dto);
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> getById(@PathVariable UUID ruleId) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(COST_SHARING_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<CostSharingRuleResponseDto>().formErrorResponse(403, "Feature enrollment.cost-sharing is not enabled"));
        }
        return costSharingRuleService.getById(ruleId);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> update(
            @PathVariable UUID companyId,
            @PathVariable UUID ruleId,
            @RequestBody @Valid CostSharingRuleRequestDto dto) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(COST_SHARING_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<CostSharingRuleResponseDto>().formErrorResponse(403, "Feature enrollment.cost-sharing is not enabled"));
        }
        return costSharingRuleService.update(ruleId, companyId, dto);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ResponseDto<String>> delete(
            @PathVariable UUID companyId,
            @PathVariable UUID ruleId) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(COST_SHARING_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<String>().formErrorResponse(403, "Feature enrollment.cost-sharing is not enabled"));
        }
        return costSharingRuleService.softDelete(ruleId, companyId);
    }
}
