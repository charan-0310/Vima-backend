package com.vimainsurance.vimaadmin.service;

import java.math.BigDecimal;
import java.util.List;

import com.vimainsurance.vimaadmin.dto.PremiumCalculationContext;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewResponseDto;

/**
 * Premium calculation engine: rate lookup and enrollment premium with employee_policy_map integration.
 */
public interface IPremiumCalculationService {

    /**
     * Look up a single rate for the given criteria. Throws if no rate found.
     */
    BigDecimal lookupRate(
            java.util.UUID companyId,
            String planType,
            String memberType,
            int memberAge,
            String coverageTier,
            BigDecimal sumInsured);

    /**
     * Calculate plan premium for given coverage and members. Uses cache for rate tables.
     */
    PlanPremiumBreakdown calculatePlanPremium(
            java.util.UUID companyId,
            String planType,
            String coverageTier,
            BigDecimal sumInsured,
            List<MemberInfo> coveredMembers);

    /**
     * Calculate full enrollment premium from context (token-derived), plan selections and dependents.
     * Resolves sum_insured and coverage_tier from employee_policy_map per plan.
     */
    PremiumCalculationResponseDto calculateEnrollmentPremium(
            PremiumCalculationContext context,
            List<PremiumCalculationRequestDto.PlanSelectionItemDto> planSelections,
            List<PremiumCalculationRequestDto.DependentItemDto> dependents);

    /**
     * Admin preview: premium with rate source and matched age band.
     */
    PremiumPreviewResponseDto previewPremium(PremiumPreviewRequestDto request);

    /** Per-plan breakdown for internal use. */
    record PlanPremiumBreakdown(
            String planType,
            BigDecimal premium,
            BigDecimal employerShare,
            BigDecimal employeeShare,
            BigDecimal gstAmount,
            String rateSource,
            String matchedAgeBand) {}

    /** Member info for rate lookup (age, type). */
    record MemberInfo(String memberType, int age, java.time.LocalDate dateOfBirth) {}
}
