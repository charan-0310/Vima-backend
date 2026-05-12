package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.IncentiveEvaluationRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveEvaluationResponseDto;
import com.vimainsurance.vimaadmin.dto.IncentivePackageDto;
import com.vimainsurance.vimaadmin.dto.IncentivePackageFullRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleFullRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleSlabDto;
import com.vimainsurance.vimaadmin.dto.IncentiveRuleSlabRequestDto;
import com.vimainsurance.vimaadmin.dto.PackageNameDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.IncentivePackage;
import com.vimainsurance.vimaadmin.entity.IncentiveRule;
import com.vimainsurance.vimaadmin.entity.IncentiveRule.RuleType;
import com.vimainsurance.vimaadmin.entity.IncentiveRuleSlab;
import com.vimainsurance.vimaadmin.repository.IIncentivePackageRepository;
import com.vimainsurance.vimaadmin.repository.IIncentiveRuleRepository;
import com.vimainsurance.vimaadmin.repository.IIncentiveRuleSlabRepository;
import com.vimainsurance.vimaadmin.service.IIncentiveService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncentiveServiceImpl implements IIncentiveService {
    private static final Logger logger = LoggerFactory.getLogger(IncentiveServiceImpl.class);
    private final IIncentivePackageRepository packageRepository;
    private final IIncentiveRuleRepository ruleRepository;
    private final IIncentiveRuleSlabRepository slabRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<List<IncentivePackageDto>>> getAllPackages() {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] getAllPackages called", correlationId);
        BaseResponse<List<IncentivePackageDto>> responseObj = new BaseResponse<>();
        List<IncentivePackageDto> packages = packageRepository.findAll().stream()
                .map(IncentivePackageDto::new)
                .collect(Collectors.toList());
        return responseObj.render(responseObj.formSuccessResponse("Packages retrieved successfully", packages));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<IncentivePackageDto>> getPackageWithRules(Long packageId) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] getPackageWithRules called with id={}", correlationId, packageId);
        BaseResponse<IncentivePackageDto> responseObj = new BaseResponse<>();
        Optional<IncentivePackage> pkgOpt = packageRepository.findById(packageId);
        return pkgOpt.map(pkg -> responseObj.render(responseObj.formSuccessResponse("Package retrieved successfully", new IncentivePackageDto(pkg))))
                .orElseGet(() -> responseObj.render(responseObj.formErrorResponse("Package not found")));
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_packages", entityType = "INCENTIVE_PACKAGE", action = "CREATE")
    public ResponseEntity<ResponseDto<IncentivePackageDto>> createFullPackage(IncentivePackageFullRequestDto requestDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] createFullPackage called", correlationId);
        BaseResponse<IncentivePackageDto> responseObj = new BaseResponse<>();
        try {
            IncentivePackage pkg = new IncentivePackage();
            pkg.setName(requestDto.getName());
            pkg.setDescription(requestDto.getDescription());
            pkg.setCreatedAt(OffsetDateTime.now());
            pkg.setUpdatedAt(OffsetDateTime.now());
            IncentivePackage savedPkg = packageRepository.save(pkg);

            if (requestDto.getRules() != null) {
                for (IncentiveRuleFullRequestDto ruleDto : requestDto.getRules()) {
                    IncentiveRule rule = new IncentiveRule();
                    rule.setPackageEntity(savedPkg);
                    rule.setRuleType(safeParseRuleType(ruleDto.getRuleType().toString()));
                    rule.setFixedPayout(ruleDto.getFixedPayout());
                    rule.setCreatedAt(OffsetDateTime.now());
                    IncentiveRule savedRule = ruleRepository.save(rule);

                    if (ruleDto.getSlabs() != null && !ruleDto.getSlabs().isEmpty()) {
                        validateSlabs(ruleDto.getSlabs());
                        List<IncentiveRuleSlab> slabs = new ArrayList<>();
                        for (IncentiveRuleSlabRequestDto slabDto : ruleDto.getSlabs()) {
                            IncentiveRuleSlab slab = new IncentiveRuleSlab();
                            slab.setRule(savedRule);
                            slab.setMinValue(slabDto.getMinValue());
                            slab.setMaxValue(slabDto.getMaxValue());
                            slab.setPayoutAmount(slabDto.getPayoutAmount());
                            slabs.add(slab);
                        }
                        slabRepository.saveAll(slabs);
                    }
                }
            }
            IncentivePackageDto dto = new IncentivePackageDto(packageRepository.findById(savedPkg.getId()).orElse(savedPkg));
            return responseObj.render(responseObj.formSuccessResponse("Package created successfully", dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] createFullPackage error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_packages", entityType = "INCENTIVE_PACKAGE", action = "UPDATE")
    public ResponseEntity<ResponseDto<IncentivePackageDto>> updateFullPackage(Long packageId, IncentivePackageFullRequestDto requestDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] updateFullPackage called with id={}", correlationId, packageId);
        BaseResponse<IncentivePackageDto> responseObj = new BaseResponse<>();
        try {
            IncentivePackage pkg = packageRepository.findById(packageId)
                    .orElseThrow(() -> new IllegalArgumentException("Incentive package not found"));
            pkg.setName(requestDto.getName());
            pkg.setDescription(requestDto.getDescription());
            pkg.setUpdatedAt(OffsetDateTime.now());
            IncentivePackage savedPkg = packageRepository.save(pkg);

            // Remove all existing rules and slabs for this package
            List<IncentiveRule> existingRules = ruleRepository.findAll().stream()
                    .filter(r -> r.getPackageEntity().getId().equals(packageId))
                    .collect(Collectors.toList());
            for (IncentiveRule rule : existingRules) {
                slabRepository.deleteByRuleId(rule.getId());
                ruleRepository.deleteById(rule.getId());
            }

            // Add new rules and slabs
            if (requestDto.getRules() != null) {
                for (IncentiveRuleFullRequestDto ruleDto : requestDto.getRules()) {
                    IncentiveRule rule = new IncentiveRule();
                    rule.setPackageEntity(savedPkg);
                    rule.setRuleType(safeParseRuleType(ruleDto.getRuleType().toString()));
                    rule.setFixedPayout(ruleDto.getFixedPayout());
                    rule.setCreatedAt(OffsetDateTime.now());
                    IncentiveRule savedRule = ruleRepository.save(rule);

                    if (ruleDto.getSlabs() != null && !ruleDto.getSlabs().isEmpty()) {
                        validateSlabs(ruleDto.getSlabs());
                        List<IncentiveRuleSlab> slabs = new ArrayList<>();
                        for (IncentiveRuleSlabRequestDto slabDto : ruleDto.getSlabs()) {
                            IncentiveRuleSlab slab = new IncentiveRuleSlab();
                            slab.setRule(savedRule);
                            slab.setMinValue(slabDto.getMinValue());
                            slab.setMaxValue(slabDto.getMaxValue());
                            slab.setPayoutAmount(slabDto.getPayoutAmount());
                            slabs.add(slab);
                        }
                        slabRepository.saveAll(slabs);
                    }
                }
            }
            IncentivePackageDto dto = new IncentivePackageDto(packageRepository.findById(savedPkg.getId()).orElse(savedPkg));
            return responseObj.render(responseObj.formSuccessResponse("Package updated successfully", dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] updateFullPackage error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_packages", entityType = "INCENTIVE_PACKAGE", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> deletePackage(Long packageId) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] deletePackage called with id={}", correlationId, packageId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (!packageRepository.existsById(packageId)) {
                throw new IllegalArgumentException("Incentive package not found");
            }
            packageRepository.deleteById(packageId);
            return responseObj.render(responseObj.formSuccessResponse("Package deleted successfully", null));
        } catch (Exception e) {
            logger.error("[correlationId:{}] deletePackage error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_rules", entityType = "INCENTIVE_RULE", action = "CREATE")
    public ResponseEntity<ResponseDto<IncentiveRuleDto>> createRule(IncentiveRuleRequestDto ruleDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] createRule called", correlationId);
        BaseResponse<IncentiveRuleDto> responseObj = new BaseResponse<>();
        try {
        IncentivePackage pkg = packageRepository.findById(ruleDto.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Incentive package not found"));
        IncentiveRule rule = new IncentiveRule();
        rule.setPackageEntity(pkg);
        rule.setRuleType(ruleDto.getRuleType());
            rule.setFixedPayout(ruleDto.getFixedPayout());
            rule.setCreatedAt(OffsetDateTime.now());
            IncentiveRule savedRule = ruleRepository.save(rule);
            if (ruleDto.getSlabs() != null && !ruleDto.getSlabs().isEmpty()) {
                validateSlabs(ruleDto.getSlabs());
                List<IncentiveRuleSlab> slabs = new ArrayList<>();
                for (IncentiveRuleSlabRequestDto slabDto : ruleDto.getSlabs()) {
                    IncentiveRuleSlab slab = new IncentiveRuleSlab();
                    slab.setRule(savedRule);
                    slab.setMinValue(slabDto.getMinValue());
                    slab.setMaxValue(slabDto.getMaxValue());
                    slab.setPayoutAmount(slabDto.getPayoutAmount());
                    slabs.add(slab);
                }
                slabRepository.saveAll(slabs);
            }
            return responseObj.render(responseObj.formSuccessResponse("Rule created successfully", new IncentiveRuleDto(savedRule)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] createRule error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_rules", entityType = "INCENTIVE_RULE", action = "UPDATE")
    public ResponseEntity<ResponseDto<IncentiveRuleDto>> updateRule(Long ruleId, IncentiveRuleRequestDto ruleDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] updateRule called with id={}", correlationId, ruleId);
        BaseResponse<IncentiveRuleDto> responseObj = new BaseResponse<>();
        try {
            IncentiveRule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Incentive rule not found"));
            rule.setRuleType(ruleDto.getRuleType());
            rule.setFixedPayout(ruleDto.getFixedPayout());
            if (ruleDto.getSlabs() != null) {
                validateSlabs(ruleDto.getSlabs());
                slabRepository.deleteByRuleId(ruleId);
                List<IncentiveRuleSlab> slabs = new ArrayList<>();
                for (IncentiveRuleSlabRequestDto slabDto : ruleDto.getSlabs()) {
                    IncentiveRuleSlab slab = new IncentiveRuleSlab();
                    slab.setRule(rule);
                    slab.setMinValue(slabDto.getMinValue());
                    slab.setMaxValue(slabDto.getMaxValue());
                    slab.setPayoutAmount(slabDto.getPayoutAmount());
                    slabs.add(slab);
                }
                slabRepository.saveAll(slabs);
            }
        IncentiveRule saved = ruleRepository.save(rule);
            return responseObj.render(responseObj.formSuccessResponse("Rule updated successfully", new IncentiveRuleDto(saved)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] updateRule error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_rules", entityType = "INCENTIVE_RULE", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> deleteRule(Long ruleId) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] deleteRule called with id={}", correlationId, ruleId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (!ruleRepository.existsById(ruleId)) {
                throw new IllegalArgumentException("Incentive rule not found");
            }
            ruleRepository.deleteById(ruleId);
            return responseObj.render(responseObj.formSuccessResponse("Rule deleted successfully", null));
        } catch (Exception e) {
            logger.error("[correlationId:{}] deleteRule error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_rule_slabs", entityType = "INCENTIVE_SLAB", action = "CREATE")
    public ResponseEntity<ResponseDto<IncentiveRuleSlabDto>> createSlab(Long ruleId, IncentiveRuleSlabRequestDto slabDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] createSlab called for ruleId={}", correlationId, ruleId);
        BaseResponse<IncentiveRuleSlabDto> responseObj = new BaseResponse<>();
        try {
            IncentiveRule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Incentive rule not found"));
            IncentiveRuleSlab slab = new IncentiveRuleSlab();
            slab.setRule(rule);
            slab.setMinValue(slabDto.getMinValue());
            slab.setMaxValue(slabDto.getMaxValue());
            slab.setPayoutAmount(slabDto.getPayoutAmount());
            IncentiveRuleSlab saved = slabRepository.save(slab);
            return responseObj.render(responseObj.formSuccessResponse("Slab created successfully", new IncentiveRuleSlabDto(saved)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] createSlab error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_rule_slabs", entityType = "INCENTIVE_SLAB", action = "UPDATE")
    public ResponseEntity<ResponseDto<IncentiveRuleSlabDto>> updateSlab(Long slabId, IncentiveRuleSlabRequestDto slabDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] updateSlab called with id={}", correlationId, slabId);
        BaseResponse<IncentiveRuleSlabDto> responseObj = new BaseResponse<>();
        try {
            IncentiveRuleSlab slab = slabRepository.findById(slabId)
                    .orElseThrow(() -> new IllegalArgumentException("Incentive rule slab not found"));
            slab.setMinValue(slabDto.getMinValue());
            slab.setMaxValue(slabDto.getMaxValue());
            slab.setPayoutAmount(slabDto.getPayoutAmount());
            IncentiveRuleSlab saved = slabRepository.save(slab);
            return responseObj.render(responseObj.formSuccessResponse("Slab updated successfully", new IncentiveRuleSlabDto(saved)));
        } catch (Exception e) {
            logger.error("[correlationId:{}] updateSlab error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "incentive_rule_slabs", entityType = "INCENTIVE_SLAB", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> deleteSlab(Long slabId) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] deleteSlab called with id={}", correlationId, slabId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (!slabRepository.existsById(slabId)) {
                throw new IllegalArgumentException("Incentive rule slab not found");
            }
            slabRepository.deleteById(slabId);
            return responseObj.render(responseObj.formSuccessResponse("Slab deleted successfully", null));
        } catch (Exception e) {
            logger.error("[correlationId:{}] deleteSlab error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<IncentiveEvaluationResponseDto>> evaluateIncentive(IncentiveEvaluationRequestDto requestDto) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] evaluateIncentive called", correlationId);
        BaseResponse<IncentiveEvaluationResponseDto> responseObj = new BaseResponse<>();
        try {
            IncentivePackage pkg = packageRepository.findById(requestDto.getPackageId())
                    .orElseThrow(() -> new IllegalArgumentException("Incentive package not found"));
            List<IncentiveEvaluationResponseDto.RuleEvaluationDto> ruleEvaluations = new ArrayList<>();
            BigDecimal totalPayout = BigDecimal.ZERO;
            for (IncentiveRule rule : pkg.getRules()) {
                BigDecimal rulePayout = calculateRulePayout(rule, requestDto);
                String appliedSlab = getAppliedSlabDescription(rule, requestDto);
                ruleEvaluations.add(new IncentiveEvaluationResponseDto.RuleEvaluationDto(
                        rule.getId(),
                        rule.getRuleType().toString(),
                        rulePayout,
                        appliedSlab
                ));
                totalPayout = totalPayout.add(rulePayout);
            }
            IncentiveEvaluationResponseDto response = new IncentiveEvaluationResponseDto(
                    pkg.getId(),
                    pkg.getName(),
                    totalPayout,
                    ruleEvaluations
            );
            return responseObj.render(responseObj.formSuccessResponse("Incentive evaluated successfully", response));
        } catch (Exception e) {
            logger.error("[correlationId:{}] evaluateIncentive error: {}", correlationId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PackageNameDto>>> getAllPackageNames() {
        BaseResponse<List<PackageNameDto>> responseObj = new BaseResponse<>();
        try {
            List<PackageNameDto> names = packageRepository.findAll()
                .stream()
                .map(pkg -> new PackageNameDto(pkg.getId(), pkg.getName()))
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse("Package names fetched", names, names.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getAllPackageNames error: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private BigDecimal calculateRulePayout(IncentiveRule rule, IncentiveEvaluationRequestDto request) {
        switch (rule.getRuleType()) {
            case RuleType.FIXED_PER_POLICY:
                return calculateFixedPerPolicyPayout(rule, request);
            case RuleType.SLAB_BASED:
                return calculateSlabBasedPayout(rule, request);
            case RuleType.PREMIUM_BASED:
                return calculatePremiumBasedPayout(rule, request);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateFixedPerPolicyPayout(IncentiveRule rule, IncentiveEvaluationRequestDto request) {
        if (rule.getFixedPayout() == null) {
            return BigDecimal.ZERO;
        }
        return rule.getFixedPayout().multiply(request.getPolicyCount());
    }

    private BigDecimal calculateSlabBasedPayout(IncentiveRule rule, IncentiveEvaluationRequestDto request) {
        List<IncentiveRuleSlab> matchingSlabs = slabRepository.findMatchingSlabs(rule.getId(), request.getPolicyCount());
        if (matchingSlabs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return matchingSlabs.get(0).getPayoutAmount();
    }

    private BigDecimal calculatePremiumBasedPayout(IncentiveRule rule, IncentiveEvaluationRequestDto request) {
        List<IncentiveRuleSlab> matchingSlabs = slabRepository.findMatchingSlabs(rule.getId(), request.getPremiumValue());
        if (matchingSlabs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return matchingSlabs.get(0).getPayoutAmount();
    }

    private String getAppliedSlabDescription(IncentiveRule rule, IncentiveEvaluationRequestDto request) {
        if (rule.getSlabs() == null || rule.getSlabs().isEmpty()) {
            return "No slabs defined";
        }
        
        BigDecimal value = switch (rule.getRuleType()) {
            case RuleType.SLAB_BASED -> request.getPolicyCount();
            case RuleType.PREMIUM_BASED -> request.getPremiumValue();
            default -> BigDecimal.ZERO;
        };
        
        List<IncentiveRuleSlab> matchingSlabs = slabRepository.findMatchingSlabs(rule.getId(), value);
        if (matchingSlabs.isEmpty()) {
            return "No matching slab";
        }
        
        IncentiveRuleSlab slab = matchingSlabs.get(0);
        return String.format("%s - %s (Payout: %s)", 
            slab.getMinValue(), 
            slab.getMaxValue() != null ? slab.getMaxValue().toString() : "∞",
            slab.getPayoutAmount());
    }

    private void validateSlabs(List<IncentiveRuleSlabRequestDto> slabs) {
        if (slabs == null || slabs.isEmpty()) {
            return;
        }
        
        // Sort slabs by min value
        List<IncentiveRuleSlabRequestDto> sortedSlabs = slabs.stream()
                .sorted(Comparator.comparing(IncentiveRuleSlabRequestDto::getMinValue))
                .collect(Collectors.toList());
        
        // Check for overlaps and validate min < max
        for (int i = 0; i < sortedSlabs.size(); i++) {
            IncentiveRuleSlabRequestDto current = sortedSlabs.get(i);
            
            // Validate min < max
            if (current.getMaxValue() != null && current.getMinValue().compareTo(current.getMaxValue()) >= 0) {
                throw new IllegalArgumentException("Min value must be less than max value");
            }
            
            // Check for overlaps with next slab
            if (i < sortedSlabs.size() - 1) {
                IncentiveRuleSlabRequestDto next = sortedSlabs.get(i + 1);
                if (current.getMaxValue() != null && next.getMinValue() != null &&
                    current.getMaxValue().compareTo(next.getMinValue()) > 0) {
                    throw new IllegalArgumentException("Slabs cannot overlap");
                }
            }
        }
    }

    private RuleType safeParseRuleType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ruleType cannot be null");
        }
        try {
            return RuleType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ruleType: " + value + ". Allowed values: SLAB_BASED, FIXED_PER_POLICY, PREMIUM_BASED");
        }
    }
} 