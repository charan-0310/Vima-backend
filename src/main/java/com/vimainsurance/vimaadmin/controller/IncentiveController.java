package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.IncentiveEvaluationRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveEvaluationResponseDto;
import com.vimainsurance.vimaadmin.dto.IncentivePackageDto;
import com.vimainsurance.vimaadmin.dto.IncentivePackageFullRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleSlabDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleSlabRequestDto;
import com.vimainsurance.vimaadmin.dto.PackageNameDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IIncentiveService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/incentives")
@RequiredArgsConstructor
public class IncentiveController {
    private static final Logger logger = LoggerFactory.getLogger(IncentiveController.class);
    private final IIncentiveService incentiveService;

    // Package endpoints
    @GetMapping("/packages")
    public ResponseEntity<ResponseDto<List<IncentivePackageDto>>> getAllPackages() {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] getAllPackages called", correlationId);
        return incentiveService.getAllPackages();
    }

    @GetMapping("/packages/{id}")
    public ResponseEntity<ResponseDto<IncentivePackageDto>> getPackageWithRules(@PathVariable Long id) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] getPackageWithRules called with id={}", correlationId, id);
        return  incentiveService.getPackageWithRules(id);

    }

    @PostMapping("/packages")
    public ResponseEntity<ResponseDto<IncentivePackageDto>> createPackage(@RequestBody IncentivePackageFullRequestDto requestDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] createPackage called", correlationId);
        return incentiveService.createFullPackage(requestDto);
    }

    @PutMapping("/packages/{id}")
    public ResponseEntity<ResponseDto<IncentivePackageDto>> updatePackage(@PathVariable Long id, @Valid @RequestBody IncentivePackageFullRequestDto requestDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] updatePackage called with id={}", correlationId, id);
        return incentiveService.updateFullPackage(id, requestDto);
    }

    @DeleteMapping("/packages/{id}")
    public ResponseEntity<ResponseDto<String>> deletePackage(@PathVariable Long id) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] deletePackage called with id={}", correlationId, id);
        return incentiveService.deletePackage(id);
    }

    @GetMapping("/packages/names")
    public ResponseEntity<ResponseDto<List<PackageNameDto>>> getAllPackageNames() {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] getAllPackageNames called", correlationId);
        return incentiveService.getAllPackageNames();
    }

    // Rule endpoints
    @PostMapping("/rules")
    public ResponseEntity<ResponseDto<IncentiveRuleDto>> createRule(@Valid @RequestBody IncentiveRuleRequestDto ruleDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] createRule called", correlationId);
        return incentiveService.createRule(ruleDto);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<ResponseDto<IncentiveRuleDto>> updateRule(@PathVariable Long id, @Valid @RequestBody IncentiveRuleRequestDto ruleDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] updateRule called with id={}", correlationId, id);
        return incentiveService.updateRule(id, ruleDto);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<ResponseDto<String>> deleteRule(@PathVariable Long id) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] deleteRule called with id={}", correlationId, id);
        return incentiveService.deleteRule(id);
    }

    // Slab endpoints
    @PostMapping("/rules/{ruleId}/slabs")
    public ResponseEntity<ResponseDto<IncentiveRuleSlabDto>> createSlab(@PathVariable Long ruleId, @Valid @RequestBody IncentiveRuleSlabRequestDto slabDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] createSlab called for ruleId={}", correlationId, ruleId);
        return incentiveService.createSlab(ruleId, slabDto);
    }

    @PutMapping("/slabs/{id}")
    public ResponseEntity<ResponseDto<IncentiveRuleSlabDto>> updateSlab(@PathVariable Long id, @Valid @RequestBody IncentiveRuleSlabRequestDto slabDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] updateSlab called with id={}", correlationId, id);
        return incentiveService.updateSlab(id, slabDto);
    }

    @DeleteMapping("/slabs/{id}")
    public ResponseEntity<ResponseDto<String>> deleteSlab(@PathVariable Long id) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] deleteSlab called with id={}", correlationId, id);
        return incentiveService.deleteSlab(id);
    }

    // Evaluation endpoint
    @PostMapping("/evaluate")
    public ResponseEntity<ResponseDto<IncentiveEvaluationResponseDto>> evaluateIncentive(@Valid @RequestBody IncentiveEvaluationRequestDto requestDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] evaluateIncentive called", correlationId);
        return incentiveService.evaluateIncentive(requestDto);
    }
} 