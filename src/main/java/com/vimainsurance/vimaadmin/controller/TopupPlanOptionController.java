package com.vimainsurance.vimaadmin.controller;

import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1")
public class TopupPlanOptionController {

    private final IEnrollmentPlanOptionsService enrollmentPlanOptionsService;

    public TopupPlanOptionController(IEnrollmentPlanOptionsService enrollmentPlanOptionsService) {
        this.enrollmentPlanOptionsService = enrollmentPlanOptionsService;
    }

    /**
     * Employee: get active plan options (Top-Up, Super Top-Up, Parent/In-Law) with premium preview.
     * Data source: product_catalog + policy tables only. Token-based, no JWT.
     */
    @GetMapping("enrollment/{token}/topup-options")
    public ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getOptionsWithPreview(@PathVariable String token) {
        return enrollmentPlanOptionsService.getActiveOptionsWithPremiumPreview(token);
    }
}
