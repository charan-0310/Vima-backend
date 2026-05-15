package com.vimainsurance.vimaadmin.service.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.dto.RateCardCoverageGapDto;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.PricingModel;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.RateSource;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IProductCatalogRepository;
import com.vimainsurance.vimaadmin.service.ICompanyEnrollmentConfigService;
import com.vimainsurance.vimaadmin.service.ICostSharingRuleService;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.PremiumRateTableCacheService;

@ExtendWith(MockitoExtension.class)
class PremiumCalculationServiceImplTest {

    private static final LocalDate PAST = LocalDate.now().minusYears(1);

    @Mock
    private PremiumRateTableCacheService cacheService;

    @Mock
    private IEmployeePolicyMapService employeePolicyMapService;

    @Mock
    private ICostSharingRuleService costSharingRuleService;

    @Mock
    private ICompanyEnrollmentConfigService companyEnrollmentConfigService;

    @Mock
    private IProductCatalogRepository productCatalogRepository;

    @Mock
    private IPolicyRepository policyRepository;

    @InjectMocks
    private PremiumCalculationServiceImpl service;

    @Test
    void findGaps_parentGmcPolicyWithOnlyGmcSelfRates_reportsGapWithParentMemberType() {
        UUID orgId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setPolicyId(42L);
        policy.setOrganizationId(orgId);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setProductType(ProductType.PARENT_GMC);

        PremiumRateTable gmcSelf = PremiumRateTable.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .productType("GMC")
                .memberType("SELF")
                .rate(new BigDecimal("5000"))
                .effectiveFrom(PAST)
                .effectiveTo(null)
                .pricingModel(PricingModel.AGE_BANDED)
                .ageBandMin(18)
                .ageBandMax(99)
                .rateSource(RateSource.NEGOTIATED)
                .build();

        when(policyRepository.findByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE)).thenReturn(List.of(policy));
        when(cacheService.getRatesForCompany(orgId)).thenReturn(List.of(gmcSelf));

        List<RateCardCoverageGapDto> gaps = service.findGapsInRateCardCoverageForActivePolicies(orgId);

        assertEquals(1, gaps.size());
        assertEquals(42L, gaps.get(0).getPolicyId());
        assertEquals("PARENT_GMC", gaps.get(0).getProductType());
        assertEquals("parent", gaps.get(0).getMemberType());
    }

    @Test
    void findGaps_parentGmcPolicyWithParentBandRows_returnsNoGaps() {
        UUID orgId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setPolicyId(7L);
        policy.setOrganizationId(orgId);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setProductType(ProductType.PARENT_GMC);

        PremiumRateTable parentRow = PremiumRateTable.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .productType("GMC")
                .memberType("parent")
                .rate(new BigDecimal("8000"))
                .effectiveFrom(PAST)
                .effectiveTo(null)
                .pricingModel(PricingModel.AGE_BANDED)
                .ageBandMin(40)
                .ageBandMax(60)
                .rateSource(RateSource.NEGOTIATED)
                .build();

        when(policyRepository.findByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE)).thenReturn(List.of(policy));
        when(cacheService.getRatesForCompany(orgId)).thenReturn(List.of(parentRow));

        assertTrue(service.findGapsInRateCardCoverageForActivePolicies(orgId).isEmpty());
    }

    @Test
    void findGaps_topUpPolicyWithGmcFallbackOnly_returnsNoGaps() {
        UUID orgId = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setPolicyId(99L);
        policy.setOrganizationId(orgId);
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setProductType(ProductType.TOP_UP);

        PremiumRateTable gmcSelf = PremiumRateTable.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .productType("GMC")
                .memberType("SELF")
                .rate(new BigDecimal("3000"))
                .effectiveFrom(PAST)
                .effectiveTo(null)
                .pricingModel(PricingModel.FLAT)
                .rateSource(RateSource.NEGOTIATED)
                .build();

        when(policyRepository.findByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE)).thenReturn(List.of(policy));
        when(cacheService.getRatesForCompany(orgId)).thenReturn(List.of(gmcSelf));

        assertTrue(service.findGapsInRateCardCoverageForActivePolicies(orgId).isEmpty());
    }

    @Test
    void findGaps_nullOrganization_returnsEmpty() {
        assertTrue(service.findGapsInRateCardCoverageForActivePolicies(null).isEmpty());
    }
}
