package com.vimainsurance.vimaadmin.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupOptionsResponseDto;
import com.vimainsurance.vimaadmin.service.IEnrollmentPlanOptionsService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/companies/{companyId}/topup-options")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class AdminTopupOptionsController {

    private final IEnrollmentPlanOptionsService enrollmentPlanOptionsService;

    public AdminTopupOptionsController(IEnrollmentPlanOptionsService enrollmentPlanOptionsService) {
        this.enrollmentPlanOptionsService = enrollmentPlanOptionsService;
    }

    @GetMapping
    public ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getOptionsWithPreview(@PathVariable UUID companyId) {
        return enrollmentPlanOptionsService.getActiveOptionsWithPremiumPreviewForCompany(companyId);
    }
}

