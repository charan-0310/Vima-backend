package com.vimainsurance.vimaadmin.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationContext;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableCsvUploadResultDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.EnrollmentTokenRateLimitService;
import com.vimainsurance.vimaadmin.service.IEnrollmentService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import com.vimainsurance.vimaadmin.service.IPremiumRateTableService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class PremiumCalculationController {

    private static final Logger logger = LoggerFactory.getLogger(PremiumCalculationController.class);

    private final IEnrollmentService enrollmentService;
    private final IPremiumCalculationService premiumCalculationService;
    private final IPremiumRateTableService premiumRateTableService;
    private final EnrollmentTokenRateLimitService rateLimitService;

    public PremiumCalculationController(
            IEnrollmentService enrollmentService,
            IPremiumCalculationService premiumCalculationService,
            IPremiumRateTableService premiumRateTableService,
            EnrollmentTokenRateLimitService rateLimitService) {
        this.enrollmentService = enrollmentService;
        this.premiumCalculationService = premiumCalculationService;
        this.premiumRateTableService = premiumRateTableService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Employee: calculate premium for enrollment (token-based). Rate limit 100 req/min per token.
     */
    @PostMapping("enrollment/{token}/calculate-premium")
    public ResponseEntity<ResponseDto<PremiumCalculationResponseDto>> calculatePremium(
            @PathVariable String token,
            @RequestBody @Valid PremiumCalculationRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/enrollment/{}/calculate-premium", MDC.get("correlationId"), token);
        BaseResponse<PremiumCalculationResponseDto> responseObj = new BaseResponse<>();
        try {
            ResponseEntity<ResponseDto<EnrollmentContextDto>> contextResp = enrollmentService.validateTokenAndGetContext(token);
            if (contextResp.getBody() == null || contextResp.getBody().getErrorCode() != null) {
                String msg = contextResp.getBody() != null ? contextResp.getBody().getMessage() : "Invalid token";
                Integer code = contextResp.getBody() != null ? contextResp.getBody().getErrorCode() : 400;
                return responseObj.render(responseObj.formErrorResponse(code != null ? code : 400, msg));
            }
            EnrollmentContextDto ctxDto = contextResp.getBody().getPayload();
            if (ctxDto == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Invalid context"));
            }
            rateLimitService.consumeOrThrow(token);
            PremiumCalculationContext context = PremiumCalculationContext.builder()
                    .companyId(ctxDto.getEnrollmentWindow() != null ? ctxDto.getEnrollmentWindow().getOrganizationId() : null)
                    .employeeId(ctxDto.getEmployeeId())
                    .employeeDateOfBirth(ctxDto.getEmployee() != null ? ctxDto.getEmployee().getDateOfBirth() : null)
                    .windowStartDate(ctxDto.getEnrollmentWindow() != null ? ctxDto.getEnrollmentWindow().getStartDate() : null)
                    .windowEndDate(ctxDto.getEnrollmentWindow() != null ? ctxDto.getEnrollmentWindow().getEndDate() : null)
                    .build();
            if (context.getCompanyId() == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Enrollment window or organization not found"));
            }
            PremiumCalculationResponseDto result = premiumCalculationService.calculateEnrollmentPremium(
                    context,
                    request.getPlanSelections(),
                    request.getDependents());
            return responseObj.render(responseObj.formSuccessResponse("OK", result));
        } catch (com.vimainsurance.vimaadmin.exception.RateLimitExceededException e) {
            return responseObj.render(responseObj.formErrorResponse(429, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("calculatePremium error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Premium calculation failed"));
        }
    }

    /**
     * Admin: preview premium with rate source and matched age band.
     */
    @PostMapping("admin/premium/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<PremiumPreviewResponseDto>> previewPremium(
            @RequestBody @Valid PremiumPreviewRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/admin/premium/preview", MDC.get("correlationId"));
        BaseResponse<PremiumPreviewResponseDto> responseObj = new BaseResponse<>();
        try {
            PremiumPreviewResponseDto result = premiumCalculationService.previewPremium(request);
            return responseObj.render(responseObj.formSuccessResponse("OK", result));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("previewPremium error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Preview failed"));
        }
    }

    @PostMapping("admin/premium/rate-tables")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> createRateTable(
            @RequestBody @Valid PremiumRateTableRequestDto dto) {
        return premiumRateTableService.create(dto);
    }

    @PutMapping("admin/premium/rate-tables/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> updateRateTable(
            @PathVariable UUID id,
            @RequestBody @Valid PremiumRateTableRequestDto dto) {
        return premiumRateTableService.update(id, dto);
    }

    @DeleteMapping("admin/premium/rate-tables/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteRateTable(@PathVariable UUID id) {
        return premiumRateTableService.softDelete(id);
    }

    @GetMapping("admin/premium/rate-tables/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> getRateTable(@PathVariable UUID id) {
        return premiumRateTableService.getById(id);
    }

    @GetMapping("admin/premium/rate-tables")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<org.springframework.data.domain.Page<PremiumRateTableResponseDto>>> listRateTables(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String planType,
            @RequestParam(required = false) String pricingModel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return premiumRateTableService.list(companyId, planType, pricingModel,
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    @PostMapping("admin/premium/rate-tables/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<PremiumRateTableCsvUploadResultDto>> uploadRateTableCsv(
            @RequestParam UUID companyId,
            @RequestParam("file") MultipartFile file) {
        return premiumRateTableService.uploadCsv(companyId, file);
    }
}
