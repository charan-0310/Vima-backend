package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.CdBalanceResponseDto;
import com.vimainsurance.vimaadmin.dto.CdAccountResponseDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionRequestDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.CdAccount;
import com.vimainsurance.vimaadmin.service.ICdAccountService;
import com.vimainsurance.vimaadmin.service.ICdBalanceService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CD (credit/debit) balance per CD account — balance read, manual transactions, ledger, recalculate.
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/cd-balance")
public class CdBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(CdBalanceController.class);

    @Autowired
    private ICdBalanceService cdBalanceService;

    @Autowired
    private ICdAccountService cdAccountService;

    /**
     * Creates (or reuses) the default CD account for the policy’s insurer and links the policy; returns the account for navigation to the ledger.
     */
    @PostMapping("/policies/{policyId}/cd-account")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<CdAccountResponseDto>> createCdAccountAndLinkPolicy(@PathVariable Long policyId) {
        logger.info("[correlationId:{}] POST /api/v1/cd-balance/policies/{}/cd-account", MDC.get("correlationId"), policyId);
        CdAccount account = cdAccountService.createDefaultAccountAndLinkPolicy(policyId);
        BaseResponse<CdAccountResponseDto> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formSuccessResponse("SUCCESS", mapCdAccount(account)));
    }

    @GetMapping("/organization/{orgId}/cd-accounts")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<CdAccountResponseDto>>> listCdAccountsByOrganization(@PathVariable java.util.UUID orgId) {
        logger.info("[correlationId:{}] GET /api/v1/cd-balance/organization/{}/cd-accounts", MDC.get("correlationId"), orgId);
        List<CdAccountResponseDto> payload = cdAccountService.listAccountsByOrganization(orgId).stream()
                .map(this::mapCdAccount)
                .collect(Collectors.toList());
        BaseResponse<List<CdAccountResponseDto>> responseObj = new BaseResponse<>();
        return responseObj.render(responseObj.formSuccessResponse("SUCCESS", payload, payload.size()));
    }

    @GetMapping("/cd-account/{cdAccountId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<CdBalanceResponseDto>> getCdBalance(@PathVariable java.util.UUID cdAccountId) {
        logger.info("[correlationId:{}] GET /api/v1/cd-balance/cd-account/{}", MDC.get("correlationId"), cdAccountId);
        return cdBalanceService.getCdBalance(cdAccountId);
    }

    @PostMapping(value = "/transaction", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<CdBalanceTransactionResponseDto>> recordTransaction(
            @Parameter(
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CdBalanceTransactionRequestDto.class)))
            @RequestPart("requestDto") CdBalanceTransactionRequestDto requestDto,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        logger.info("[correlationId:{}] POST /api/v1/cd-balance/transaction cdAccountId={}", MDC.get("correlationId"),
                requestDto != null ? requestDto.getCdAccountId() : null);
        return cdBalanceService.recordTransaction(requestDto, files);
    }

    @GetMapping("/cd-account/{cdAccountId}/ledger")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<CdBalanceTransactionResponseDto>>> getTransactionLedger(
            @PathVariable java.util.UUID cdAccountId,
            @RequestParam(required = false) Long policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        logger.info("[correlationId:{}] GET /api/v1/cd-balance/cd-account/{}/ledger policyId={} page={} size={} type={} dateFrom={} dateTo={}",
                MDC.get("correlationId"), cdAccountId, policyId, page, size, type, dateFrom, dateTo);
        return cdBalanceService.getTransactionLedger(cdAccountId, policyId, page, size, type, dateFrom, dateTo);
    }

    @PostMapping("/cd-account/{cdAccountId}/recalculate")
    @PreAuthorize("hasRole('SUPER_ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<CdBalanceResponseDto>> recalculateBalance(@PathVariable java.util.UUID cdAccountId) {
        logger.info("[correlationId:{}] POST /api/v1/cd-balance/cd-account/{}/recalculate", MDC.get("correlationId"), cdAccountId);
        return cdBalanceService.recalculateBalance(cdAccountId);
    }

    private CdAccountResponseDto mapCdAccount(CdAccount account) {
        CdAccountResponseDto dto = new CdAccountResponseDto();
        dto.setCdAccountId(account.getCdAccountId());
        dto.setOrganizationId(account.getOrganizationId());
        dto.setInsurerName(account.getInsurerName());
        dto.setLabel(account.getLabel());
        dto.setCdBalance(account.getCdBalance());
        dto.setStatus(account.getStatus() != null ? account.getStatus().name() : null);
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }
}
