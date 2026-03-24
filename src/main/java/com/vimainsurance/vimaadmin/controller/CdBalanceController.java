package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.util.List;

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
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionRequestDto;
import com.vimainsurance.vimaadmin.dto.CdBalanceTransactionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.ICdBalanceService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CD (credit/debit) balance per policy — balance read, manual transactions, ledger, recalculate.
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/cd-balance")
public class CdBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(CdBalanceController.class);

    @Autowired
    private ICdBalanceService cdBalanceService;

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN',)")
    public ResponseEntity<ResponseDto<CdBalanceResponseDto>> getCdBalance(@PathVariable Long policyId) {
        logger.info("[correlationId:{}] GET /api/v1/cd-balance/policy/{}", MDC.get("correlationId"), policyId);
        return cdBalanceService.getCdBalance(policyId);
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
        logger.info("[correlationId:{}] POST /api/v1/cd-balance/transaction policyId={}", MDC.get("correlationId"),
                requestDto != null ? requestDto.getPolicyId() : null);
        return cdBalanceService.recordTransaction(requestDto, files);
    }

    @GetMapping("/policy/{policyId}/ledger")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<CdBalanceTransactionResponseDto>>> getTransactionLedger(
            @PathVariable Long policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        logger.info("[correlationId:{}] GET /api/v1/cd-balance/policy/{}/ledger page={} size={} type={} dateFrom={} dateTo={}",
                MDC.get("correlationId"), policyId, page, size, type, dateFrom, dateTo);
        return cdBalanceService.getTransactionLedger(policyId, page, size, type, dateFrom, dateTo);
    }

    @PostMapping("/policy/{policyId}/recalculate")
    @PreAuthorize("hasRole('SUPER_ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<CdBalanceResponseDto>> recalculateBalance(@PathVariable Long policyId) {
        logger.info("[correlationId:{}] POST /api/v1/cd-balance/policy/{}/recalculate", MDC.get("correlationId"), policyId);
        return cdBalanceService.recalculateBalance(policyId);
    }
}
