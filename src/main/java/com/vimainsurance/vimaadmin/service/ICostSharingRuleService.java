package com.vimainsurance.vimaadmin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CostShareSplit;
import com.vimainsurance.vimaadmin.dto.CostSharingRuleRequestDto;
import com.vimainsurance.vimaadmin.dto.CostSharingRuleResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;

public interface ICostSharingRuleService {

    ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> create(CostSharingRuleRequestDto dto);

    ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> update(UUID ruleId, UUID companyId, CostSharingRuleRequestDto dto);

    ResponseEntity<ResponseDto<String>> softDelete(UUID ruleId, UUID companyId);

    ResponseEntity<ResponseDto<CostSharingRuleResponseDto>> getById(UUID ruleId);

    ResponseEntity<ResponseDto<List<CostSharingRuleResponseDto>>> listByCompany(UUID companyId, String planType, LocalDate effectiveDate);

    CostSharingRule getEffectiveRule(UUID companyId, String planType, String coverageCategory, LocalDate date);

    CostShareSplit applyCostSharing(UUID companyId, String planType, String coverageCategory, BigDecimal totalPremium);

    /**
     * Returns deduction amounts per frequency (MONTHLY, QUARTERLY, YEARLY) for the given annual employee share.
     * Values rounded to nearest rupee. For proration, pass prorated annual when applicable.
     */
    Map<String, BigDecimal> calculateDeductions(BigDecimal annualEmployeeShare);
}
