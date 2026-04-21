package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.dto.CostShareSplit;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationContext;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumCalculationResponseDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumPreviewResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.entity.ProductCatalog;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.RateSource;
import com.vimainsurance.vimaadmin.service.ICompanyEnrollmentConfigService;
import com.vimainsurance.vimaadmin.service.ICostSharingRuleService;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import com.vimainsurance.vimaadmin.service.PremiumRateTableCacheService;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IProductCatalogRepository;
import com.vimainsurance.vimaadmin.util.TopupPremiumOptionsUtil;
import com.vimainsurance.vimaadmin.service.policy.PolicyMemberMappingHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PremiumCalculationServiceImpl implements IPremiumCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PremiumCalculationServiceImpl.class);
    private static final LocalDate TODAY = LocalDate.now();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, BigDecimal>> MAP_STRING_BIG_DECIMAL = new TypeReference<>() {};

    /** GHI and GMC are both group health; rate table may use either. TOP_UP/SUPER_TOP_UP use GMC rate when no dedicated rate exists. PARENT_GMC uses GMC rates with member_type parent. */
    private static boolean planTypeMatchesRateProductType(String requestPlanType, String rateProductType) {
        if (requestPlanType == null || rateProductType == null) return false;
        String r = requestPlanType.trim().toUpperCase();
        String p = rateProductType.trim().toUpperCase();
        if (r.equals(p)) return true;
        if (("GHI".equals(r) && "GMC".equals(p)) || ("GMC".equals(r) && "GHI".equals(p))) return true;
        // Top-up options use base health (GMC) rate for premium preview when no TOP_UP/SUPER_TOP_UP rate row exists
        if (("TOP_UP".equals(r) || "SUPER_TOP_UP".equals(r)) && ("GMC".equals(p) || "GHI".equals(p))) return true;
        if (("GMC".equals(r) || "GHI".equals(r)) && ("TOP_UP".equals(p) || "SUPER_TOP_UP".equals(p))) return true;
        // Parent cover: use GMC rate table rows with member_type parent/parent_in_law
        if ("PARENT_GMC".equals(r) && ("GMC".equals(p) || "GHI".equals(p))) return true;
        return false;
    }

    private final PremiumRateTableCacheService cacheService;
    private final IEmployeePolicyMapService employeePolicyMapService;
    private final ICostSharingRuleService costSharingRuleService;
    private final ICompanyEnrollmentConfigService companyEnrollmentConfigService;
    private final IProductCatalogRepository productCatalogRepository;
    private final IPolicyRepository policyRepository;

    @Override
    public BigDecimal lookupRate(UUID companyId, String planType, String memberType, int memberAge,
            String coverageTier, BigDecimal sumInsured) {
        List<PremiumRateTable> rates = cacheService.getRatesForCompany(companyId);
        List<PremiumRateTable> forPlan = rates.stream()
                .filter(r -> planTypeMatchesRateProductType(planType, r.getProductType()))
                .filter(r -> r.getEffectiveFrom() != null && !r.getEffectiveFrom().isAfter(TODAY))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(TODAY))
                .toList();
        if (forPlan.isEmpty()) {
            throw new IllegalArgumentException("No rate found for company " + companyId + ", plan " + planType);
        }
        forPlan = sortRatesDeterministically(forPlan);
        PricingModel model = resolveEffectiveModel(forPlan);
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
                .filter(r -> planTypeMatchesRateProductType(planType, r.getProductType()))
                .filter(r -> r.getEffectiveFrom() != null && !r.getEffectiveFrom().isAfter(TODAY))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(TODAY))
                .toList();
        if (forPlan.isEmpty()) {
            throw new IllegalArgumentException("No rate found for company " + companyId + ", plan " + planType);
        }
        forPlan = sortRatesDeterministically(forPlan);
        // Prefer dedicated TOP_UP/SUPER_TOP_UP rate when present; otherwise use GMC fallback
        String planUpper = planType != null ? planType.trim().toUpperCase() : "";
        if ("TOP_UP".equals(planUpper) || "SUPER_TOP_UP".equals(planUpper)) {
            List<PremiumRateTable> dedicated = forPlan.stream()
                    .filter(rt -> planUpper.equals(rt.getProductType() != null ? rt.getProductType().trim().toUpperCase() : ""))
                    .toList();
            if (!dedicated.isEmpty()) {
                forPlan = dedicated;
            }
        }
        // PARENT_GMC: use GMC rate rows with member_type parent or parent_in_law (age-banded parent rates)
        if ("PARENT_GMC".equals(planUpper)) {
            List<PremiumRateTable> parentRates = forPlan.stream()
                    .filter(rt -> {
                        String mt = rt.getMemberType() != null ? rt.getMemberType().trim().toLowerCase() : "";
                        return "parent".equals(mt) || "parent_in_law".equals(mt);
                    })
                    .toList();
            if (!parentRates.isEmpty()) {
                forPlan = parentRates;
            } else {
                throw new IllegalArgumentException("No parent rate found for company " + companyId + "; add premium_rate_tables with product_type GMC and member_type 'parent' (or 'parent_in_law') for age bands.");
            }
        }
        forPlan = sortRatesDeterministically(forPlan);
        PricingModel model = resolveEffectiveModel(forPlan);
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
            String mt = row.getMemberType() != null ? row.getMemberType().trim().toUpperCase() : "";
            if ("FAMILY".equals(mt)) {
                if (row.getFamilySizeMax() != null) {
                    int familySize = Math.max(1, coveredMembers.size());
                    if (row.getFamilySizeMin() != null && familySize < row.getFamilySizeMin()) {
                        throw new IllegalArgumentException(
                                "Family size " + familySize + " below minimum " + row.getFamilySizeMin());
                    }
                    if (familySize > row.getFamilySizeMax()) {
                        throw new IllegalArgumentException(
                                "Family size " + familySize + " exceeds maximum " + row.getFamilySizeMax());
                    }
                }
                totalPremium = row.getRate();
            } else {
                totalPremium = row.getRate().multiply(BigDecimal.valueOf(Math.max(1, coveredMembers.size())));
            }
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
        CompanyEnrollmentConfigResponseDto config = companyEnrollmentConfigService.getConfigForCompany(context.getCompanyId());
        List<EmployeePolicyMapResponseDto> familyMappings = getEmployeeMappings(context.getEmployeeId());
        boolean parentCoveredByBaseGmc = isParentCoveredByBaseGmc(
                planSelections,
                familyMappings,
                context.getEmployeeId(),
                context.getCompanyId());
        validateParentDependents(dependents, config, parentCoveredByBaseGmc);
        List<MemberInfo> members = buildMemberList(context.getEmployeeDateOfBirth(), dependents);

        BigDecimal totalAnnual = BigDecimal.ZERO;
        BigDecimal totalEmployer = BigDecimal.ZERO;
        BigDecimal totalEmployee = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        List<PremiumCalculationResponseDto.PlanBreakdownItemDto> breakdowns = new ArrayList<>();

        for (PremiumCalculationRequestDto.PlanSelectionItemDto sel : planSelections) {
            if (!Boolean.TRUE.equals(sel.getOpted())) continue;
            String selPlanType = sel.getPlanType();
            String planUpper = selPlanType != null ? selPlanType.trim().toUpperCase() : "";

            BigDecimal sumInsured = sel.getSumInsured();
            String coverageTier = sel.getCoverageTier();
            EmployeePolicyMapResponseDto mapping = familyMappings.stream()
                    .filter(m -> context.getEmployeeId().equals(m.getIndividualId()))
                    .filter(m -> planTypeMatchesRateProductType(selPlanType, m.getProductType()))
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

            // PARENT_GMC: only parent/in-law. TOP_UP / SUPER_TOP_UP / GPA / GTL: employee only.
            // GMC / GHI: floater. For ESCP tier, parents are part of base GMC (not separate PARENT_GMC).
            List<MemberInfo> membersForPlan = members;
            if ("PARENT_GMC".equals(planUpper)) {
                List<MemberInfo> parentOnly = members.stream()
                        .filter(m -> {
                            String mt = m.memberType();
                            return mt != null && ("parent".equalsIgnoreCase(mt) || "parent_in_law".equalsIgnoreCase(mt));
                        })
                        .toList();
                if (parentOnly.isEmpty()) continue; // no parent dependents, skip this selection
                membersForPlan = parentOnly;
            } else if ("TOP_UP".equals(planUpper) || "SUPER_TOP_UP".equals(planUpper)) {
                MemberInfo employeeOnly = members.stream()
                        .filter(m -> "EMPLOYEE".equalsIgnoreCase(m.memberType()) || "self".equalsIgnoreCase(m.memberType()))
                        .findFirst()
                        .orElse(members.isEmpty() ? null : members.get(0));
                membersForPlan = employeeOnly != null ? List.of(employeeOnly) : members;
            } else if ("GPA".equals(planUpper) || "GTL".equals(planUpper)) {
                MemberInfo employeeOnly = members.stream()
                        .filter(m -> "EMPLOYEE".equalsIgnoreCase(m.memberType()) || "self".equalsIgnoreCase(m.memberType()))
                        .findFirst()
                        .orElse(members.isEmpty() ? null : members.get(0));
                membersForPlan = employeeOnly != null ? List.of(employeeOnly) : members;
            } else if ("GMC".equals(planUpper) || "GHI".equals(planUpper)) {
                membersForPlan = isEscpCoverageTier(coverageTier)
                        ? members
                        : PolicyMemberMappingHelper.membersForGmcFloater(members);
            }
            try {
                PlanPremiumBreakdown b;
                if (("TOP_UP".equals(planUpper) || "SUPER_TOP_UP".equals(planUpper)) && sel.getTopupPlanOptionId() != null) {
                    Optional<BigDecimal> fixedOpt = resolveFixedTopupPremium(
                            sel.getTopupPlanOptionId(), context.getCompanyId(), sumInsured);
                    if (fixedOpt.isPresent()) {
                        BigDecimal p = fixedOpt.get().setScale(2, RoundingMode.HALF_UP);
                        b = new PlanPremiumBreakdown(
                                selPlanType,
                                p,
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                p,
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                "POLICY_FIXED",
                                null);
                    } else {
                        throw new IllegalArgumentException(
                                "Fixed Top-Up premium mapping not found for selected plan option and sum insured.");
                    }
                } else {
                    b = calculatePlanPremium(
                            context.getCompanyId(),
                            selPlanType,
                            coverageTier,
                            sumInsured,
                            membersForPlan);
                }
                BigDecimal planTotalPremium = b.premium();
                String coverageCategory = resolveCoverageCategoryForCostSharing(membersForPlan);
                var split = costSharingRuleService.applyCostSharing(
                        context.getCompanyId(),
                        selPlanType,
                        coverageCategory,
                        planTotalPremium);
                // Default 50/50 for PARENT_GMC when no cost-sharing rule (employer pays 100% otherwise)
                if ("PARENT_GMC".equals(planUpper)
                        && split.getEmployeeShare() != null && split.getEmployeeShare().compareTo(BigDecimal.ZERO) == 0
                        && split.getEmployerShare() != null && split.getEmployerShare().compareTo(planTotalPremium) == 0) {
                    BigDecimal half = planTotalPremium.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    split = CostShareSplit.builder()
                            .employerShare(half)
                            .employeeShare(planTotalPremium.subtract(half).setScale(2, RoundingMode.HALF_UP))
                            .shareType(split.getShareType())
                            .shareValue(split.getShareValue())
                            .ruleId(split.getRuleId())
                            .build();
                }
                // Voluntary add-ons (TOP_UP, SUPER_TOP_UP) are always 100% employee-paid
                if ("TOP_UP".equals(planUpper) || "SUPER_TOP_UP".equals(planUpper)) {
                    split = CostShareSplit.builder()
                            .employerShare(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .employeeShare(planTotalPremium.setScale(2, RoundingMode.HALF_UP))
                            .shareType(split.getShareType())
                            .shareValue(split.getShareValue())
                            .ruleId(split.getRuleId())
                            .build();
                }
                totalAnnual = totalAnnual.add(planTotalPremium);
                totalEmployer = totalEmployer.add(split.getEmployerShare());
                totalEmployee = totalEmployee.add(split.getEmployeeShare());
                totalGst = totalGst.add(b.gstAmount());
                breakdowns.add(PremiumCalculationResponseDto.PlanBreakdownItemDto.builder()
                        .planType(b.planType())
                        .premium(planTotalPremium)
                        .employerShare(split.getEmployerShare())
                        .employeeShare(split.getEmployeeShare())
                        .gstAmount(b.gstAmount())
                        .build());
            } catch (Exception e) {
                log.warn("Plan premium calculation failed for {}: {}", selPlanType, e.getMessage());
            }
        }

        Map<String, BigDecimal> deductionOptions = costSharingRuleService.calculateDeductions(totalEmployee);

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

    /** Admin-defined SI↔premium pairs on policy (via product catalog id). */
    private Optional<BigDecimal> resolveFixedTopupPremium(UUID catalogId, UUID companyId, BigDecimal sumInsured) {
        if (catalogId == null || companyId == null || sumInsured == null) {
            return Optional.empty();
        }
        Optional<ProductCatalog> pcOpt = productCatalogRepository.findById(catalogId);
        if (pcOpt.isEmpty()) {
            return Optional.empty();
        }
        ProductCatalog pc = pcOpt.get();
        if (pc.getOrganizationId() == null || !pc.getOrganizationId().equals(companyId)) {
            return Optional.empty();
        }
        if (pc.getPolicyId() == null) {
            return Optional.empty();
        }
        Map<BigDecimal, BigDecimal> catalogPreview = parsePremiumPreviewOptionsMap(pc.getPremiumPreviewOptions());
        if (!catalogPreview.isEmpty()) {
            BigDecimal catalogPremium = catalogPreview.get(sumInsured);
            if (catalogPremium != null) {
                return Optional.of(catalogPremium);
            }
        }
        Optional<Policy> polOpt = policyRepository.findById(pc.getPolicyId());
        if (polOpt.isEmpty()) {
            return Optional.empty();
        }
        Policy pol = polOpt.get();
        return TopupPremiumOptionsUtil.findPremiumForSumInsured(
                pol.getSumInsuredOptions(), pol.getTopupPremiumOptions(), sumInsured);
    }

    private static Map<BigDecimal, BigDecimal> parsePremiumPreviewOptionsMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, BigDecimal> raw = OBJECT_MAPPER.readValue(json, MAP_STRING_BIG_DECIMAL);
            if (raw == null || raw.isEmpty()) {
                return Map.of();
            }
            Map<BigDecimal, BigDecimal> out = new LinkedHashMap<>();
            raw.forEach((k, v) -> {
                if (k == null || k.isBlank() || v == null) {
                    return;
                }
                try {
                    out.put(new BigDecimal(k), v);
                } catch (NumberFormatException ignored) {
                }
            });
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<EmployeePolicyMapResponseDto> getEmployeeMappings(UUID employeeId) {
        ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> resp = employeePolicyMapService.getMappingsForEmployeeFamily(employeeId);
        ResponseDto<List<EmployeePolicyMapResponseDto>> body = resp != null ? resp.getBody() : null;
        if (body == null || body.getPayload() == null) {
            return List.of();
        }
        return body.getPayload();
    }

    private void validateParentDependents(List<PremiumCalculationRequestDto.DependentItemDto> dependents,
            CompanyEnrollmentConfigResponseDto config, boolean parentCoveredByBaseGmc) {
        if (dependents == null || dependents.isEmpty()) return;
        int parentCount = 0;
        int inLawCount = 0;
        Integer ageLimit = config.getParentAgeLimit();
        // Treat 0/negative as "not configured" to avoid blocking all parent ages.
        if (ageLimit != null && ageLimit <= 0) {
            ageLimit = null;
        }
        int maxParents = config.getMaxParents() != null ? config.getMaxParents() : 0;
        int maxInLaws = config.getMaxInLaws() != null ? config.getMaxInLaws() : 0;
        boolean parentEnabled = Boolean.TRUE.equals(config.getParentCoverageEnabled());
        boolean inLawEnabled = Boolean.TRUE.equals(config.getInLawCoverageEnabled());
        for (PremiumCalculationRequestDto.DependentItemDto d : dependents) {
            String rel = d.getRelationship() != null ? d.getRelationship().trim().toUpperCase() : "";
            boolean isParent = "PARENT".equals(rel) || "FATHER".equals(rel) || "MOTHER".equals(rel);
            boolean isParentInLaw = "PARENT_IN_LAW".equals(rel) || "FATHER-IN-LAW".equals(rel) || "MOTHER-IN-LAW".equals(rel);
            if (parentCoveredByBaseGmc && (isParent || isParentInLaw)) {
                // ESCP base GMC includes parents within core family coverage.
                continue;
            }
            if (isParent) {
                if (!parentEnabled) throw new IllegalArgumentException("Parent coverage is not enabled for this company");
                if (d.getDateOfBirth() == null) {
                    throw new IllegalArgumentException("Parent date of birth is required for premium calculation");
                }
                parentCount++;
                if (maxParents > 0 && parentCount > maxParents)
                    throw new IllegalArgumentException("Maximum number of parents allowed is " + maxParents);
                if (ageLimit != null && d.getDateOfBirth() != null) {
                    int age = Period.between(d.getDateOfBirth(), TODAY).getYears();
                    if (age > ageLimit)
                        throw new IllegalArgumentException("Parent age must not exceed " + ageLimit + " years");
                }
            } else if (isParentInLaw) {
                if (!inLawEnabled) throw new IllegalArgumentException("Parent-in-law coverage is not enabled for this company");
                if (d.getDateOfBirth() == null) {
                    throw new IllegalArgumentException("Parent-in-law date of birth is required for premium calculation");
                }
                inLawCount++;
                if (maxInLaws > 0 && inLawCount > maxInLaws)
                    throw new IllegalArgumentException("Maximum number of parents-in-law allowed is " + maxInLaws);
                if (ageLimit != null && d.getDateOfBirth() != null) {
                    int age = Period.between(d.getDateOfBirth(), TODAY).getYears();
                    if (age > ageLimit)
                        throw new IllegalArgumentException("Parent-in-law age must not exceed " + ageLimit + " years");
                }
            }
        }
    }

    private boolean isParentCoveredByBaseGmc(List<PremiumCalculationRequestDto.PlanSelectionItemDto> planSelections,
            List<EmployeePolicyMapResponseDto> familyMappings, UUID employeeId, UUID companyId) {
        boolean hasOptedBaseHealthPlan = false;
        if (planSelections == null || planSelections.isEmpty()) {
            return false;
        }
        for (PremiumCalculationRequestDto.PlanSelectionItemDto sel : planSelections) {
            if (!Boolean.TRUE.equals(sel.getOpted()) || sel.getPlanType() == null) {
                continue;
            }
            String planUpper = sel.getPlanType().trim().toUpperCase();
            if (!"GMC".equals(planUpper) && !"GHI".equals(planUpper)) {
                continue;
            }
            hasOptedBaseHealthPlan = true;
            String coverageTier = sel.getCoverageTier();
            if (coverageTier == null) {
                coverageTier = familyMappings.stream()
                        .filter(m -> employeeId.equals(m.getIndividualId()))
                        .filter(m -> planTypeMatchesRateProductType(sel.getPlanType(), m.getProductType()))
                        .map(EmployeePolicyMapResponseDto::getCoverageTier)
                        .findFirst()
                        .orElse(null);
            }
            if (isEscpCoverageTier(coverageTier)) {
                return true;
            }
        }
        return hasOptedBaseHealthPlan && isEscpBasePolicyForCompany(companyId);
    }

    private boolean isEscpCoverageTier(String coverageTier) {
        return coverageTier != null && "ESCP".equalsIgnoreCase(coverageTier.trim());
    }

    private boolean isEscpBasePolicyForCompany(UUID companyId) {
        List<Policy> gmcPolicies = policyRepository.findByOrganizationIdAndProductTypeAndStatus(
                companyId,
                ProductType.GMC,
                PolicyStatus.ACTIVE);
        boolean gmcEscp = gmcPolicies.stream()
                .anyMatch(p -> p.getCoverageType() == CoverageType.ESCP);
        if (gmcEscp) {
            return true;
        }
        List<Policy> ghiPolicies = policyRepository.findByOrganizationIdAndProductTypeAndStatus(
                companyId,
                ProductType.GHI,
                PolicyStatus.ACTIVE);
        return ghiPolicies.stream().anyMatch(p -> p.getCoverageType() == CoverageType.ESCP);
    }

    private String resolveCoverageCategoryForCostSharing(List<MemberInfo> members) {
        if (members == null || members.size() <= 1) return CoverageCategory.SELF.getValue();
        boolean hasParent = false;
        boolean hasParentInLaw = false;
        for (MemberInfo m : members) {
            String t = m.memberType();
            if ("parent".equalsIgnoreCase(t)) hasParent = true;
            if ("parent_in_law".equalsIgnoreCase(t)) hasParentInLaw = true;
        }
        if (hasParent) return CoverageCategory.PARENT.getValue();
        if (hasParentInLaw) return CoverageCategory.PARENT_IN_LAW.getValue();
        return CoverageCategory.ALL_DEPENDENTS.getValue();
    }

    private static PricingModel resolveEffectiveModel(List<PremiumRateTable> rows) {
        if (rows == null || rows.isEmpty()) {
            return PricingModel.FLAT;
        }
        for (PremiumRateTable row : rows) {
            if (row.getPricingModel() != null) {
                return row.getPricingModel();
            }
        }
        return PricingModel.FLAT;
    }

    private static List<PremiumRateTable> sortRatesDeterministically(List<PremiumRateTable> rows) {
        return rows.stream()
                .sorted(Comparator
                        .comparing((PremiumRateTable r) -> nullSafeString(r.getProductType()))
                        .thenComparing(r -> nullSafeString(r.getMemberType()))
                        .thenComparing(r -> r.getPricingModel() != null ? r.getPricingModel().getValue() : "")
                        .thenComparing(r -> r.getAgeBandMin() != null ? r.getAgeBandMin() : Integer.MIN_VALUE)
                        .thenComparing(r -> r.getAgeBandMax() != null ? r.getAgeBandMax() : Integer.MAX_VALUE)
                        .thenComparing(r -> r.getFamilySizeMin() != null ? r.getFamilySizeMin() : Integer.MIN_VALUE)
                        .thenComparing(r -> r.getFamilySizeMax() != null ? r.getFamilySizeMax() : Integer.MAX_VALUE)
                        .thenComparing(r -> r.getEffectiveFrom() != null ? r.getEffectiveFrom() : LocalDate.MIN)
                        .thenComparing(r -> r.getEffectiveTo() != null ? r.getEffectiveTo() : LocalDate.MAX)
                        .thenComparing(r -> r.getCreatedAt() != null ? r.getCreatedAt() : java.time.LocalDateTime.MIN)
                        .thenComparing(r -> r.getId() != null ? r.getId().toString() : ""))
                .toList();
    }

    private static String nullSafeString(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private List<MemberInfo> buildMemberList(LocalDate employeeDob, List<PremiumCalculationRequestDto.DependentItemDto> dependents) {
        List<MemberInfo> list = new ArrayList<>();
        int employeeAge = employeeDob != null ? Period.between(employeeDob, TODAY).getYears() : 30;
        list.add(new MemberInfo("EMPLOYEE", Math.max(0, employeeAge), employeeDob));
        if (dependents != null) {
            for (PremiumCalculationRequestDto.DependentItemDto d : dependents) {
                int age = d.getDateOfBirth() != null ? Period.between(d.getDateOfBirth(), TODAY).getYears() : 0;
                String memberType = toRateTableMemberType(d.getRelationship());
                list.add(new MemberInfo(memberType, Math.max(0, age), d.getDateOfBirth()));
            }
        }
        return list;
    }

    /** Map relationship to rate table member_type (parent, parent_in_law for age-banded parent rates). */
    private static String toRateTableMemberType(String relationship) {
        if (relationship == null || relationship.isBlank()) return "DEPENDENT";
        String r = relationship.trim().toUpperCase();
        if ("PARENT".equals(r) || "FATHER".equals(r) || "MOTHER".equals(r)) return "parent";
        if ("PARENT_IN_LAW".equals(r) || "FATHER-IN-LAW".equals(r) || "MOTHER-IN-LAW".equals(r)) return "parent_in_law";
        return relationship;
    }
}
