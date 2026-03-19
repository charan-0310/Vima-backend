package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.CostShareSplit;
import com.vimainsurance.vimaadmin.dto.CostSharingRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.CostSharingRuleResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.enums.DeductionFrequency;
import com.vimainsurance.vimaadmin.enums.EmployerShareType;
import com.vimainsurance.vimaadmin.mapper.CostSharingRuleMapper;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;
import com.vimainsurance.vimaadmin.service.CostSharingRuleCacheService;
import com.vimainsurance.vimaadmin.service.ICostSharingRuleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CostSharingRuleServiceImpl implements ICostSharingRuleService {

    private static final Logger log = LoggerFactory.getLogger(CostSharingRuleServiceImpl.class);

    /** GHI and GMC are both group health; match so enrollment request finds cost-sharing rules. */
    private static boolean planTypeMatches(String requestPlanType, String rulePlanType) {
        if (requestPlanType == null || rulePlanType == null) return false;
        String r = requestPlanType.trim().toUpperCase();
        String p = rulePlanType.trim().toUpperCase();
        if (r.equals(p)) return true;
        return ("GHI".equals(r) && "GMC".equals(p)) || ("GMC".equals(r) && "GHI".equals(p));
    }

    private final ICostSharingRuleRepository repository;
    private final CostSharingRuleCacheService cacheService;

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "cost_sharing_rules", entityType = "COST_SHARING_RULE", action = "CREATE")
    public ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> create(CostSharingRuleRequestDto dto) {
        BaseResponse<CostSharingRuleResponseDto> responseObj = new BaseResponse<>();
        try {
            validatePercentage(dto);
            CostSharingRule entity = CostSharingRuleMapper.toEntity(dto);
            entity = repository.save(entity);
            cacheService.invalidate(dto.getCompanyId());
            return responseObj.render(responseObj.formSuccessResponse("Cost-sharing rule created", CostSharingRuleMapper.toResponseDto(entity)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("create cost-sharing rule error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to create cost-sharing rule"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "cost_sharing_rules", entityType = "COST_SHARING_RULE", action = "UPDATE")
    public ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> update(UUID ruleId, UUID companyId, CostSharingRuleRequestDto dto) {
        BaseResponse<CostSharingRuleResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<CostSharingRule> opt = repository.findById(ruleId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Cost-sharing rule not found"));
            }
            CostSharingRule entity = opt.get();
            if (!entity.getCompanyId().equals(companyId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Rule does not belong to this company"));
            }
            validatePercentage(dto);
            entity.setPlanType(dto.getPlanType());
            entity.setCoverageCategory(dto.getCoverageCategory());
            entity.setEmployerShareType(dto.getEmployerShareType());
            entity.setEmployerShareValue(dto.getEmployerShareValue());
            entity.setExcessAllowed(Boolean.TRUE.equals(dto.getExcessAllowed()));
            entity.setEffectiveFrom(dto.getEffectiveFrom());
            entity.setEffectiveTo(dto.getEffectiveTo());
            entity = repository.save(entity);
            cacheService.invalidate(companyId);
            return responseObj.render(responseObj.formSuccessResponse("Cost-sharing rule updated", CostSharingRuleMapper.toResponseDto(entity)));
        } catch (IllegalArgumentException e) {
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("update cost-sharing rule error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to update cost-sharing rule"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "cost_sharing_rules", entityType = "COST_SHARING_RULE", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> softDelete(UUID ruleId, UUID companyId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<CostSharingRule> opt = repository.findById(ruleId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Cost-sharing rule not found"));
            }
            if (!opt.get().getCompanyId().equals(companyId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Rule does not belong to this company"));
            }
            int updated = repository.softDeleteById(ruleId);
            if (updated > 0) {
                cacheService.invalidate(companyId);
                return responseObj.render(responseObj.formSuccessResponse("Cost-sharing rule deleted", "OK"));
            }
            return responseObj.render(responseObj.formErrorResponse("Failed to delete cost-sharing rule"));
        } catch (Exception e) {
            log.error("softDelete cost-sharing rule error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to delete cost-sharing rule"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> getById(UUID ruleId) {
        BaseResponse<CostSharingRuleResponseDto> responseObj = new BaseResponse<>();
        try {
            return repository.findById(ruleId)
                    .map(CostSharingRuleMapper::toResponseDto)
                    .map(dto -> responseObj.render(responseObj.formSuccessResponse("OK", dto)))
                    .orElseGet(() -> responseObj.render(responseObj.formErrorResponse(404, "Cost-sharing rule not found")));
        } catch (Exception e) {
            log.error("getById cost-sharing rule error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get cost-sharing rule"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<CostSharingRuleResponseDto>>> listByCompany(UUID companyId, String planType, LocalDate effectiveDate) {
        BaseResponse<List<CostSharingRuleResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<CostSharingRule> rules;
            if (effectiveDate != null) {
                rules = repository.findActiveRulesForCompany(companyId, effectiveDate);
            } else {
                rules = repository.findByCompanyId(companyId);
            }
            if (planType != null && !planType.isBlank()) {
                rules = rules.stream().filter(r -> planType.equals(r.getPlanType())).toList();
            }
            List<CostSharingRuleResponseDto> dtos = rules.stream().map(CostSharingRuleMapper::toResponseDto).toList();
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            log.error("listByCompany cost-sharing rules error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to list cost-sharing rules"));
        }
    }

    @Override
    public CostSharingRule getEffectiveRule(UUID companyId, String planType, String coverageCategory, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<CostSharingRule> rules = cacheService.getRulesForCompany(companyId);
        CoverageCategory category = parseCoverageCategory(coverageCategory);
        List<CostSharingRule> forPlanAndDate = rules.stream()
                .filter(r -> planTypeMatches(planType, r.getPlanType()))
                .filter(r -> !r.getEffectiveFrom().isAfter(effectiveDate))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(effectiveDate))
                .toList();
        if (category != null) {
            Optional<CostSharingRule> exact = forPlanAndDate.stream()
                    .filter(r -> r.getCoverageCategory() == category)
                    .findFirst();
            if (exact.isPresent()) {
                return exact.get();
            }
        }
        return forPlanAndDate.stream()
                .filter(r -> r.getCoverageCategory() == CoverageCategory.ALL_DEPENDENTS)
                .findFirst()
                .orElse(null);
    }

    @Override
    public CostShareSplit applyCostSharing(UUID companyId, String planType, String coverageCategory, BigDecimal totalPremium) {
        if (totalPremium == null || totalPremium.compareTo(BigDecimal.ZERO) <= 0) {
            return CostShareSplit.builder()
                    .employerShare(BigDecimal.ZERO)
                    .employeeShare(BigDecimal.ZERO)
                    .shareType(null)
                    .shareValue(null)
                    .ruleId(null)
                    .build();
        }
        CostSharingRule rule = getEffectiveRule(companyId, planType, coverageCategory, LocalDate.now());
        if (rule == null) {
            return CostShareSplit.builder()
                    .employerShare(totalPremium.setScale(2, RoundingMode.HALF_UP))
                    .employeeShare(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .shareType(null)
                    .shareValue(null)
                    .ruleId(null)
                    .build();
        }
        BigDecimal employerShare;
        if (rule.getEmployerShareType() == EmployerShareType.PERCENTAGE) {
            employerShare = totalPremium.multiply(rule.getEmployerShareValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            employerShare = rule.getEmployerShareValue().min(totalPremium).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal employeeShare = totalPremium.subtract(employerShare).setScale(2, RoundingMode.HALF_UP);
        if (employeeShare.compareTo(BigDecimal.ZERO) < 0) {
            employeeShare = BigDecimal.ZERO;
            employerShare = totalPremium.setScale(2, RoundingMode.HALF_UP);
        }
        return CostShareSplit.builder()
                .employerShare(employerShare)
                .employeeShare(employeeShare)
                .shareType(rule.getEmployerShareType())
                .shareValue(rule.getEmployerShareValue())
                .ruleId(rule.getId())
                .build();
    }

    @Override
    public Map<String, BigDecimal> calculateDeductions(BigDecimal annualEmployeeShare) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (annualEmployeeShare == null || annualEmployeeShare.compareTo(BigDecimal.ZERO) <= 0) {
            result.put(DeductionFrequency.MONTHLY.getValue(), BigDecimal.ZERO);
            result.put(DeductionFrequency.QUARTERLY.getValue(), BigDecimal.ZERO);
            result.put(DeductionFrequency.YEARLY.getValue(), BigDecimal.ZERO);
            return result;
        }
        result.put(DeductionFrequency.MONTHLY.getValue(),
                annualEmployeeShare.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP));
        result.put(DeductionFrequency.QUARTERLY.getValue(),
                annualEmployeeShare.divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP));
        result.put(DeductionFrequency.YEARLY.getValue(), annualEmployeeShare.setScale(0, RoundingMode.HALF_UP));
        return result;
    }

    private static void validatePercentage(CostSharingRuleRequestDto dto) {
        if (dto.getEmployerShareType() == EmployerShareType.PERCENTAGE
                && (dto.getEmployerShareValue() == null
                || dto.getEmployerShareValue().compareTo(BigDecimal.ZERO) < 0
                || dto.getEmployerShareValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Employer share percentage must be between 0 and 100");
        }
    }

    private static CoverageCategory parseCoverageCategory(String coverageCategory) {
        if (coverageCategory == null || coverageCategory.isBlank()) {
            return null;
        }
        try {
            return CoverageCategory.fromValue(coverageCategory);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
