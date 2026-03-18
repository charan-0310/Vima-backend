package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
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
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupOptionsResponseDto;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.ProductCatalog;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IProductCatalogRepository;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IEnrollmentPlanOptionsService;
import com.vimainsurance.vimaadmin.service.IEnrollmentService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnrollmentPlanOptionsServiceImpl implements IEnrollmentPlanOptionsService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentPlanOptionsServiceImpl.class);
    private static final List<String> ENROLLMENT_PLAN_PRODUCT_TYPES = List.of("TOP_UP", "SUPER_TOP_UP", "PARENT_GMC");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<BigDecimal>> LIST_BIG_DECIMAL = new TypeReference<>() {};

    private final IEnrollmentService enrollmentService;
    private final IEmployeePolicyMapService employeePolicyMapService;
    private final IPremiumCalculationService premiumCalculationService;
    private final IProductCatalogRepository productCatalogRepository;
    private final IPolicyRepository policyRepository;

    @Override
    public ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getActiveOptionsWithPremiumPreview(String enrollmentToken) {
        BaseResponse<TopupOptionsResponseDto> responseObj = new BaseResponse<>();
        try {
            ResponseEntity<ResponseDto<EnrollmentContextDto>> contextResp = enrollmentService.validateTokenAndGetContext(enrollmentToken);
            if (contextResp.getBody() == null || contextResp.getBody().getErrorCode() != null) {
                String msg = contextResp.getBody() != null ? contextResp.getBody().getMessage() : "Invalid token";
                Integer code = contextResp.getBody() != null ? contextResp.getBody().getErrorCode() : 400;
                return responseObj.render(responseObj.formErrorResponse(code != null ? code : 400, msg));
            }
            EnrollmentContextDto ctx = contextResp.getBody().getPayload();
            if (ctx == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Invalid context"));
            }
            UUID companyId = ctx.getEnrollmentWindow() != null ? ctx.getEnrollmentWindow().getOrganizationId() : null;
            if (companyId == null) {
                return responseObj.render(responseObj.formErrorResponse(400, "Enrollment window or organization not found"));
            }
            boolean hasGmcFromOrg = ctx.getOrganizationPolicies() != null && ctx.getOrganizationPolicies().stream()
                    .anyMatch(p -> p != null && ("GMC".equalsIgnoreCase(p.getProductType()) || "GHI".equalsIgnoreCase(p.getProductType())));
            boolean hasGmcFromMapping = false;
            if (!hasGmcFromOrg) {
                ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> mapResp =
                        employeePolicyMapService.getMappingsForEmployeeFamily(ctx.getEmployeeId());
                List<EmployeePolicyMapResponseDto> maps =
                        mapResp.getBody() != null && mapResp.getBody().getPayload() != null
                                ? mapResp.getBody().getPayload() : List.of();
                hasGmcFromMapping = maps.stream()
                        .filter(m -> companyId.equals(m.getOrganizationId()))
                        .anyMatch(m -> "GMC".equalsIgnoreCase(m.getProductType()));
            }
            if (!hasGmcFromOrg && !hasGmcFromMapping) {
                return responseObj.render(responseObj.formErrorResponse(400, "Base GMC coverage is required before adding top-up options"));
            }
            LocalDate today = LocalDate.now();
            LocalDate employeeDob = ctx.getEmployee() != null ? ctx.getEmployee().getDateOfBirth() : null;
            int employeeAge = employeeDob != null ? Period.between(employeeDob, today).getYears() : 0;
            List<IPremiumCalculationService.MemberInfo> memberList = List.of(
                    new IPremiumCalculationService.MemberInfo("self", employeeAge, employeeDob));

            List<TopupOptionsResponseDto.TopupOptionWithPreview> withPreviews = new ArrayList<>();
            List<ProductCatalog> catalogList = productCatalogRepository.findByOrganizationIdAndIsActive(companyId, true);
            for (ProductCatalog pc : catalogList) {
                if (pc.getProductType() == null || !ENROLLMENT_PLAN_PRODUCT_TYPES.contains(pc.getProductType().toUpperCase())) {
                    continue;
                }
                if (pc.getEffectiveFrom() != null && today.isBefore(pc.getEffectiveFrom())) {
                    continue;
                }
                if (pc.getEffectiveTo() != null && today.isAfter(pc.getEffectiveTo())) {
                    continue;
                }
                List<BigDecimal> sumInsuredList = parseSumInsuredFromJson(pc.getCoverageOptions());
                boolean isParentGmc = "PARENT_GMC".equalsIgnoreCase(pc.getProductType());
                if (sumInsuredList.isEmpty() && !isParentGmc) {
                    continue;
                }
                if (sumInsuredList.isEmpty() && isParentGmc) {
                    sumInsuredList = List.of(BigDecimal.ONE);
                }
                BigDecimal deductibleAmount = null;
                if (pc.getPolicyId() != null) {
                    Optional<Policy> policyOpt = policyRepository.findById(pc.getPolicyId());
                    if (policyOpt.isPresent()) {
                        deductibleAmount = policyOpt.get().getDeductibleAmount();
                    }
                }
                Map<BigDecimal, BigDecimal> premiumPreview = new LinkedHashMap<>();
                for (BigDecimal si : sumInsuredList) {
                    try {
                        var breakdown = premiumCalculationService.calculatePlanPremium(
                                companyId, pc.getProductType(), "INDIVIDUAL", si, memberList);
                        premiumPreview.put(si, breakdown.premium());
                    } catch (Exception e) {
                        log.debug("No premium rate for product_catalog {} sumInsured {}: {}", pc.getId(), si, e.getMessage());
                    }
                }
                withPreviews.add(TopupOptionsResponseDto.TopupOptionWithPreview.builder()
                        .id(pc.getId())
                        .planType(pc.getProductType())
                        .name(pc.getName())
                        .deductibleAmount(deductibleAmount)
                        .sumInsuredOptions(sumInsuredList)
                        .premiumPreviewPerOption(premiumPreview)
                        .build());
            }

            // PARENT_GMC: fetch directly from policy table (design: PARENT_GMC stored only in policy table).
            // For each PARENT_GMC policy without a product_catalog row, create one so it appears in enrollment plans.
            List<Policy> parentGmcPolicies = policyRepository.findByOrganizationIdAndProductTypeAndStatus(
                    companyId, ProductType.PARENT_GMC, PolicyStatus.ACTIVE);
            for (Policy policy : parentGmcPolicies) {
                if (policy.getPolicyId() == null) continue;
                LocalDate effFrom = policy.getEffectiveFrom() != null ? policy.getEffectiveFrom() : policy.getStartDate();
                LocalDate effTo = policy.getEffectiveTo() != null ? policy.getEffectiveTo() : policy.getEndDate();
                if (effFrom != null && today.isBefore(effFrom)) continue;
                if (effTo != null && today.isAfter(effTo)) continue;
                Optional<ProductCatalog> existing = productCatalogRepository.findByOrganizationIdAndPolicyId(companyId, policy.getPolicyId());
                ProductCatalog pc;
                if (existing.isPresent()) {
                    // Already in product_catalog and already added in the first loop above; skip to avoid duplicate.
                    continue;
                }
                pc = ensureProductCatalogForParentGmcPolicy(policy, companyId);
                if (pc == null) continue;
                List<BigDecimal> sumInsuredList = List.of(BigDecimal.ONE);
                Map<BigDecimal, BigDecimal> premiumPreview = new LinkedHashMap<>();
                withPreviews.add(TopupOptionsResponseDto.TopupOptionWithPreview.builder()
                        .id(pc.getId())
                        .planType("PARENT_GMC")
                        .name(pc.getName())
                        .deductibleAmount(null)
                        .sumInsuredOptions(sumInsuredList)
                        .premiumPreviewPerOption(premiumPreview)
                        .build());
            }

            TopupOptionsResponseDto result = TopupOptionsResponseDto.builder().options(withPreviews).build();
            return responseObj.render(responseObj.formSuccessResponse("OK", result));
        } catch (Exception e) {
            log.error("getActiveOptionsWithPremiumPreview error: {}", e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get top-up options"));
        }
    }

    /**
     * Creates a product_catalog row for a PARENT_GMC policy so it appears in enrollment plans.
     * Used when the policy exists in the policy table but has no catalog entry (e.g. created before catalog sync).
     */
    private ProductCatalog ensureProductCatalogForParentGmcPolicy(Policy policy, UUID companyId) {
        if (policy.getPolicyId() == null || policy.getOrganizationId() == null) return null;
        LocalDate effectiveFrom = policy.getEffectiveFrom() != null ? policy.getEffectiveFrom() : policy.getStartDate();
        if (effectiveFrom == null) effectiveFrom = LocalDate.now();
        LocalDate effectiveTo = policy.getEffectiveTo() != null ? policy.getEffectiveTo() : policy.getEndDate();
        String name = (policy.getDescription() != null && !policy.getDescription().isBlank())
                ? policy.getDescription()
                : "Parent / In-Law Coverage";
        if (name.length() > 255) name = name.substring(0, 255);
        List<ProductCatalog> existing = productCatalogRepository.findByOrganizationIdOrderByDisplayOrderAsc(companyId);
        int displayOrder = existing.isEmpty() ? 0
                : (existing.get(existing.size() - 1).getDisplayOrder() == null ? 0 : existing.get(existing.size() - 1).getDisplayOrder()) + 1;
        ProductCatalog catalog = ProductCatalog.builder()
                .organizationId(companyId)
                .productType("PARENT_GMC")
                .name(name)
                .isMandatory(false)
                .coverageOptions("[1]")
                .displayOrder(displayOrder)
                .policyId(policy.getPolicyId())
                .isActive(true)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .build();
        catalog = productCatalogRepository.save(catalog);
        log.info("Created product_catalog for PARENT_GMC policyId={} organizationId={}", policy.getPolicyId(), companyId);
        return catalog;
    }

    private static List<BigDecimal> parseSumInsuredFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, LIST_BIG_DECIMAL);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
