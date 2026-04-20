package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessPartnerResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessRedirectResponseDto;
import com.vimainsurance.vimaadmin.service.IWellnessPartnerService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/employee/wellness/partners")
public class EmployeeWellnessController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeWellnessController.class);

    private final IWellnessPartnerService wellnessPartnerService;

    public EmployeeWellnessController(IWellnessPartnerService wellnessPartnerService) {
        this.wellnessPartnerService = wellnessPartnerService;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ResponseDto<List<WellnessPartnerResponseDto>>> getPartners(
            @RequestParam(required = false) String category) {
        logger.info("[correlationId:{}] GET /api/v1/employee/wellness/partners called", MDC.get("correlationId"));
        return wellnessPartnerService.getEmployeePartners(category);
    }

    @PostMapping("/{partnerSlug}/redirect")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ResponseDto<WellnessRedirectResponseDto>> getRedirectUrl(
            @PathVariable String partnerSlug) {
        logger.info("[correlationId:{}] POST /api/v1/employee/wellness/partners/{}/redirect called",
                MDC.get("correlationId"), partnerSlug);
        return wellnessPartnerService.getEmployeeRedirectUrl(partnerSlug);
    }
}
