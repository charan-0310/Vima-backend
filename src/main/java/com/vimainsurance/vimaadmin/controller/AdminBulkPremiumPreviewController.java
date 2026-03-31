package com.vimainsurance.vimaadmin.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.BulkEmployeePremiumPreviewRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.serviceimpl.EmployeeService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/companies/{companyId}/bulk/premium-preview")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class AdminBulkPremiumPreviewController {

    private final EmployeeService employeeService;

    public AdminBulkPremiumPreviewController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<ResponseDto<PremiumCalculationResponseDto>> preview(
            @PathVariable UUID companyId,
            @Valid @RequestBody BulkEmployeePremiumPreviewRequestDto request) {
        return employeeService.previewBulkEmployeePremium(companyId, request);
    }
}

