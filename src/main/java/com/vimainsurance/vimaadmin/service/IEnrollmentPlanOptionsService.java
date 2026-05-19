package com.vimainsurance.vimaadmin.service;

import org.springframework.http.ResponseEntity;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupOptionsResponseDto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for enrollment plan options (Top-Up, Super Top-Up, Parent/In-Law) from product_catalog + policy only.
 */
public interface IEnrollmentPlanOptionsService {

    /**
     * Returns active plan options with premium preview for the enrollment token's organization.
     * Data source: product_catalog and policy tables only (no topup_plan_options).
     */
    ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getActiveOptionsWithPremiumPreview(
            String enrollmentToken, HttpServletRequest request);

    /**
     * Admin: Returns active plan options with premium preview for a company (JWT-protected).
     * Intended for Bulk Upload / Manual Add plan selection and validation.
     */
    ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getActiveOptionsWithPremiumPreviewForCompany(UUID companyId);
}
