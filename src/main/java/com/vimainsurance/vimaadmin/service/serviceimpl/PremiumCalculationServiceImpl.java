package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationContext;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.RateSource;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import com.vimainsurance.vimaadmin.service.PremiumRateTableCacheService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PremiumCalculationServiceImpl implements IPremiumCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PremiumCalculationServiceImpl.class);
    private static final LocalDate TODAY = LocalDate.now();

    private final PremiumRateTableCacheService cacheService;
    private final IEmployeePolicyMapService employeePolicyMapService;

    @Override
    public BigDecimal lookupRate(UUID companyId, String planType, String memberType, int memberAge,
            String coverageTier, BigDecimal sumInsured) {
        List<PremiumRateTable> rates = cacheService.getRatesForCompany(companyId);
        List<PremiumRateTable> forPlan = rates.stream()
                .filter(r -> planType.equals(r.getProductType()))
                .filter(r -> r.getEffectiveFrom() != null && !r.getEffectiveFrom().isAfter(TODAY))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(TODAY))
                .toList();
        if (forPlan.isEmpty()) {
            throw new IllegalArgumentException("No rate found for company " + companyId + ", plan " + planType);
        }
        PricingModel model = forPlan.get(0).getPricingModel();
        if (model == null) {
            model = PricingModel.FLAT;
        }
        if (model == PricingModel.FLAT) {
            return forPlan.stream()
                    .filter(r -> (memberType == null || memberType.equals(r.getMemberType())))
                    .findFirst()
                    .map(PremiumRateTable::getRate)
                    .orElseThrow(() -> new IllegalArgumentException("No flat rate found for company " + companyId + ", plan " + planType));
        }
        if (model == PricingModel.AGE_BANDED) {
            return forPlan.stream()
                    .filter(r -> (memberType == null || memberType.equals(r.getMemberType())))
                    .filter(r -> r.getAgeBandMin() != null && r.getAgeBandMin() <= memberAge)
                    .filter(r -> r.getAgeBandMax() == null || r.getAgeBandMax() > memberAge)
                    .findFirst()
                    .map(PremiumRateTable::getRate)
                    .orElseThrow(() -> new IllegalArgumentException("No age-banded rate found for age " + memberAge + ", plan " + planType));
        }
        throw new IllegalArgumentException("lookupRate does not support pricing model " + model + "; use calculatePlanPremium for family floater");
    }

    @Override
    public PlanPremiumBreakdown calculatePlanPremium(UUID companyId, String planType, String coverageTier,
            BigDecimal sumInsured, List<MemberInfo> coveredMembers) {
        List<PremiumRateTable> rates = cacheService.getRatesForCompany(companyId);
        List<PremiumRateTable> forPlan = rates.stream()
                .filter(r -> planType.equals(r.getProductType()))
                .filter(r -> r.getEffectiveFrom() != null && !r.getEffectiveFrom().isAfter(TODAY))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(TODAY))
                .toList();
        if (forPlan.isEmpty()) {
            throw new IllegalArgumentException("No rate found for company " + companyId + ", plan " + planType);
        }
        PricingModel model = forPlan.get(0).getPricingModel();
        if (model == null) {
            model = PricingModel.FLAT;
        }
        BigDecimal totalPremium = BigDecimal.ZERO;
        String matchedAgeBand = null;
        RateSource rateSource = forPlan.get(0).getRateSource();
        boolean gstInclusive = Boolean.TRUE.equals(forPlan.get(0).getGstInclusive());
        BigDecimal gstPct = forPlan.get(0).getGstPercentage() != null ? forPlan.get(0).getGstPercentage() : BigDecimal.ZERO;

        if (model == PricingModel.FLAT) {
            PremiumRateTable row = forPlan.stream().findFirst().orElseThrow();
            totalPremium = row.getRate().multiply(BigDecimal.valueOf(Math.max(1, coveredMembers.size())));
            if (row.getAgeBandMin() != null) {
                matchedAgeBand = row.getAgeBandMin() + "-" + (row.getAgeBandMax() != null ? row.getAgeBandMax() : "");
            }
        } else if (model == PricingModel.AGE_BANDED) {
            for (MemberInfo m : coveredMembers) {
                PremiumRateTable row = forPlan.stream()
                        .filter(r -> r.getAgeBandMin() != null && r.getAgeBandMin() <= m.age())
                        .filter(r -> r.getAgeBandMax() == null || r.getAgeBandMax() > m.age())
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No rate for age " + m.age() + ", plan " + planType));
                totalPremium = totalPremium.add(row.getRate());
                if (matchedAgeBand == null && row.getAgeBandMin() != null) {
                    matchedAgeBand = row.getAgeBandMin() + "-" + (row.getAgeBandMax() != null ? row.getAgeBandMax() : "");
                }
            }
        } else if (model == PricingModel.FAMILY_FLOATER) {
            int familySize = Math.max(1, coveredMembers.size());
            PremiumRateTable row = forPlan.stream()
                    .filter(r -> r.getFamilySizeMin() != null && r.getFamilySizeMin() <= familySize)
                    .filter(r -> r.getFamilySizeMax() == null || r.getFamilySizeMax() >= familySize)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No family floater rate for size " + familySize + ", plan " + planType));
            totalPremium = row.getRate();
            if (row.getAgeBandMin() != null) {
                matchedAgeBand = row.getAgeBandMin() + "-" + (row.getAgeBandMax() != null ? row.getAgeBandMax() : "");
            }
        }

        BigDecimal gstAmount = BigDecimal.ZERO;
        if (gstPct.compareTo(BigDecimal.ZERO) > 0) {
            if (gstInclusive) {
                gstAmount = totalPremium.multiply(gstPct).divide(BigDecimal.valueOf(100).add(gstPct), 2, RoundingMode.HALF_UP);
            } else {
                gstAmount = totalPremium.multiply(gstPct).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
            }
        }
        BigDecimal employerShare = totalPremium.multiply(BigDecimal.valueOf(70)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employeeShare = totalPremium.subtract(employerShare).setScale(2, RoundingMode.HALF_UP);
        return new PlanPremiumBreakdown(
                planType,
                totalPremium.setScale(2, RoundingMode.HALF_UP),
                employerShare,
                employeeShare,
                gstAmount,
                rateSource != null ? rateSource.getValue() : null,
                matchedAgeBand);
    }

    @Override
    public PremiumCalculationResponseDto calculateEnrollmentPremium(
            PremiumCalculationContext context,
            List<PremiumCalculationRequestDto.PlanSelectionItemDto> planSelections,
            List<PremiumCalculationRequestDto.DependentItemDto> dependents) {
        List<EmployeePolicyMapResponseDto> familyMappings = getEmployeeMappings(context.getEmployeeId());
        List<MemberInfo> members = buildMemberList(context.getEmployeeDateOfBirth(), dependents);

        BigDecimal totalAnnual = BigDecimal.ZERO;
        BigDecimal totalEmployer = BigDecimal.ZERO;
        BigDecimal totalEmployee = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        List<PremiumCalculationResponseDto.PlanBreakdownItemDto> breakdowns = new ArrayList<>();

        for (PremiumCalculationRequestDto.PlanSelectionItemDto sel : planSelections) {
            if (!Boolean.TRUE.equals(sel.getOpted())) continue;
            BigDecimal sumInsured = sel.getSumInsured();
            String coverageTier = sel.getCoverageTier();
            EmployeePolicyMapResponseDto mapping = familyMappings.stream()
                    .filter(m -> context.getEmployeeId().equals(m.getIndividualId()))
                    .filter(m -> sel.getPlanType().equals(m.getProductType()))
                    .findFirst()
                    .orElse(null);
            if (mapping != null) {
                if (sumInsured == null && mapping.getSumInsured() != null) {
                    sumInsured = mapping.getSumInsured();
                }
                if (coverageTier == null && mapping.getCoverageTier() != null) {
                    coverageTier = mapping.getCoverageTier();
                }
            }
            if (sumInsured == null) sumInsured = BigDecimal.valueOf(500000);
            try {
                PlanPremiumBreakdown b = calculatePlanPremium(
                        context.getCompanyId(),
                        sel.getPlanType(),
                        coverageTier,
                        sumInsured,
                        members);
                totalAnnual = totalAnnual.add(b.premium());
                totalEmployer = totalEmployer.add(b.employerShare());
                totalEmployee = totalEmployee.add(b.employeeShare());
                totalGst = totalGst.add(b.gstAmount());
                breakdowns.add(PremiumCalculationResponseDto.PlanBreakdownItemDto.builder()
                        .planType(b.planType())
                        .premium(b.premium())
                        .employerShare(b.employerShare())
                        .employeeShare(b.employeeShare())
                        .gstAmount(b.gstAmount())
                        .build());
            } catch (Exception e) {
                log.warn("Plan premium calculation failed for {}: {}", sel.getPlanType(), e.getMessage());
            }
        }

        int remainingMonths = 1;
        if (context.getWindowEndDate() != null) {
            remainingMonths = Math.max(1, Period.between(TODAY, context.getWindowEndDate()).getMonths()
                    + 12 * Period.between(TODAY, context.getWindowEndDate()).getYears());
        }
        BigDecimal proratedAnnual = totalAnnual.multiply(BigDecimal.valueOf(remainingMonths)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> deductionOptions = new HashMap<>();
        deductionOptions.put("MONTHLY", proratedAnnual.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
        deductionOptions.put("YEARLY", totalAnnual);

        return PremiumCalculationResponseDto.builder()
                .totalAnnualPremium(totalAnnual)
                .totalEmployerShare(totalEmployer)
                .totalEmployeeShare(totalEmployee)
                .gstAmount(totalGst)
                .perPlanBreakdown(breakdowns)
                .deductionOptions(deductionOptions)
                .build();
    }

    @Override
    public PremiumPreviewResponseDto previewPremium(PremiumPreviewRequestDto request) {
        if (request.getCompanyId() == null || request.getPlanType() == null) {
            throw new IllegalArgumentException("companyId and planType required");
        }
        List<MemberInfo> members = new ArrayList<>();
        if (request.getMembers() != null) {
            for (PremiumPreviewRequestDto.MemberAgeDto m : request.getMembers()) {
                int age = m.getAge() != null ? m.getAge() : (m.getDateOfBirth() != null ? Period.between(m.getDateOfBirth(), TODAY).getYears() : 0);
                members.add(new MemberInfo(m.getMemberType() != null ? m.getMemberType() : "EMPLOYEE", age, m.getDateOfBirth()));
            }
        }
        if (members.isEmpty()) {
            members.add(new MemberInfo("EMPLOYEE", 30, null));
        }
        BigDecimal sumInsured = request.getSumInsured() != null ? request.getSumInsured() : BigDecimal.valueOf(500000);
        PlanPremiumBreakdown b = calculatePlanPremium(
                request.getCompanyId(),
                request.getPlanType(),
                request.getCoverageTier(),
                sumInsured,
                members);
        String matchedBand = b.matchedAgeBand();
        Integer bandMin = null, bandMax = null;
        if (matchedBand != null && matchedBand.contains("-")) {
            String[] parts = matchedBand.split("-");
            if (parts.length >= 2) {
                try {
                    bandMin = Integer.parseInt(parts[0].trim());
                    bandMax = parts[1].trim().isEmpty() ? null : Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return PremiumPreviewResponseDto.builder()
                .totalPremium(b.premium())
                .gstAmount(b.gstAmount())
                .rateSource(b.rateSource() != null ? RateSource.fromValue(b.rateSource()) : null)
                .matchedAgeBand(matchedBand)
                .matchedAgeBandMin(bandMin)
                .matchedAgeBandMax(bandMax)
                .build();
    }

    private List<EmployeePolicyMapResponseDto> getEmployeeMappings(UUID employeeId) {
        ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> resp = employeePolicyMapService.getMappingsForEmployeeFamily(employeeId);
        ResponseDto<List<EmployeePolicyMapResponseDto>> body = resp != null ? resp.getBody() : null;
        if (body == null || body.getPayload() == null) {
            return List.of();
        }
        return body.getPayload();
    }

    private List<MemberInfo> buildMemberList(LocalDate employeeDob, List<PremiumCalculationRequestDto.DependentItemDto> dependents) {
        List<MemberInfo> list = new ArrayList<>();
        int employeeAge = employeeDob != null ? Period.between(employeeDob, TODAY).getYears() : 30;
        list.add(new MemberInfo("EMPLOYEE", Math.max(0, employeeAge), employeeDob));
        if (dependents != null) {
            for (PremiumCalculationRequestDto.DependentItemDto d : dependents) {
                int age = d.getDateOfBirth() != null ? Period.between(d.getDateOfBirth(), TODAY).getYears() : 0;
                list.add(new MemberInfo(d.getRelationship() != null ? d.getRelationship() : "DEPENDENT", Math.max(0, age), d.getDateOfBirth()));
            }
        }
        return list;
    }
}
