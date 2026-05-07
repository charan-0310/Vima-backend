package com.vimainsurance.vimaadmin.service.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.vimainsurance.vimaadmin.dto.CostShareSplit;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.enums.EmployerShareType;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;
import com.vimainsurance.vimaadmin.service.CostSharingRuleCacheService;

/**
 * Tests for CostSharingRuleServiceImpl, focused on the lookup fallback chain.
 *
 * The bug being protected against: an employee with spouse + child resolves to coverage
 * category ALL_DEPENDENTS, but HR may have only configured a SELF rule. Before the fix,
 * the lookup returned null and applyCostSharing fell back to "100% employer / 0% employee",
 * so the magic-link UI showed "you pay 0" everywhere.
 *
 * The fallback chain is now: exact match → ALL_DEPENDENTS → SELF → null.
 * We do NOT fall back to "any rule" because that would risk applying e.g. a PARENT-only
 * rule to a SELF employee.
 */
@ExtendWith(MockitoExtension.class)
class CostSharingRuleServiceImplTest {

    @Mock
    private ICostSharingRuleRepository repository;

    @Mock
    private CostSharingRuleCacheService cacheService;

    @InjectMocks
    private CostSharingRuleServiceImpl service;

    private static final UUID COMPANY = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now();

    private static CostSharingRule rule(String planType, CoverageCategory category, int employerPct, LocalDate effectiveFrom) {
        CostSharingRule r = new CostSharingRule();
        r.setId(UUID.randomUUID());
        r.setCompanyId(COMPANY);
        r.setPlanType(planType);
        r.setCoverageCategory(category);
        r.setEmployerShareType(EmployerShareType.PERCENTAGE);
        r.setEmployerShareValue(BigDecimal.valueOf(employerPct));
        r.setEffectiveFrom(effectiveFrom);
        return r;
    }

    @Test
    void getEffectiveRule_returnsSelfRule_whenLookupIsAllDependentsAndOnlySelfRuleExists() {
        // The headline bug: HR sets only SELF=80/20. Employee with spouse+child hits the
        // resolver which returns ALL_DEPENDENTS. Lookup must fall back to SELF, not null.
        when(cacheService.getRulesForCompany(COMPANY))
                .thenReturn(List.of(rule("GMC", CoverageCategory.SELF, 80, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "ALL_DEPENDENTS", TODAY);

        assertNotNull(found, "should fall back to SELF when ALL_DEPENDENTS not configured");
        assertEquals(CoverageCategory.SELF, found.getCoverageCategory());
        assertEquals(0, BigDecimal.valueOf(80).compareTo(found.getEmployerShareValue()));
    }

    @Test
    void getEffectiveRule_picksDependentRule_whenLookupIsAllDependentsAndOnlyPerCategoryDependentRulesExist() {
        // The user's actual production config: SELF=100/0 (default, untouched) and
        // SPOUSE/CHILD/PARENT all at 80/20. Employee with spouse + 2 children → resolver
        // returns ALL_DEPENDENTS. Without dependent-rule fallback, we'd land on SELF=100/0
        // (the original symptom). Must instead pick the dependent rule.
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of(
                rule("GMC", CoverageCategory.SELF, 100, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.SPOUSE, 80, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.CHILD, 80, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.PARENT, 80, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "ALL_DEPENDENTS", TODAY);

        assertNotNull(found);
        // Must pick a dependent rule (80/20), not SELF (100/0).
        assertEquals(0, BigDecimal.valueOf(80).compareTo(found.getEmployerShareValue()),
                "ALL_DEPENDENTS lookup must prefer dependent rules over SELF");
        // SPOUSE has highest priority in the dependent-fallback list.
        assertEquals(CoverageCategory.SPOUSE, found.getCoverageCategory());
    }

    @Test
    void getEffectiveRule_returnsAllDependentsRule_whenLookupIsSelfAndOnlyAllDependentsRuleExists() {
        when(cacheService.getRulesForCompany(COMPANY))
                .thenReturn(List.of(rule("GMC", CoverageCategory.ALL_DEPENDENTS, 75, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "SELF", TODAY);

        assertNotNull(found);
        assertEquals(CoverageCategory.ALL_DEPENDENTS, found.getCoverageCategory());
    }

    @Test
    void getEffectiveRule_prefersExactCategoryMatch_overFallback() {
        // When both exact and fallback rules exist, exact match wins.
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of(
                rule("GMC", CoverageCategory.SELF, 80, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.ALL_DEPENDENTS, 60, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "ALL_DEPENDENTS", TODAY);

        assertEquals(CoverageCategory.ALL_DEPENDENTS, found.getCoverageCategory());
        assertEquals(0, BigDecimal.valueOf(60).compareTo(found.getEmployerShareValue()));
    }

    @Test
    void getEffectiveRule_returnsNull_whenOnlyParentRuleExistsAndLookupIsSelf() {
        // We must NOT apply a PARENT-only rule to a SELF employee. Returning null
        // makes applyCostSharing fall back to 100% employer (correct default).
        when(cacheService.getRulesForCompany(COMPANY))
                .thenReturn(List.of(rule("GMC", CoverageCategory.PARENT, 75, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "SELF", TODAY);

        assertNull(found, "PARENT-only rule must not be applied to SELF lookup");
    }

    @Test
    void getEffectiveRule_picksMostRecentEffectiveDate_whenDuplicatesExist() {
        // Defensive: even if duplicates ever exist, prefer the most recent configuration.
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of(
                rule("GMC", CoverageCategory.SELF, 100, TODAY.minusDays(30)),
                rule("GMC", CoverageCategory.SELF, 80, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "SELF", TODAY);

        assertNotNull(found);
        assertEquals(0, BigDecimal.valueOf(80).compareTo(found.getEmployerShareValue()),
                "should prefer the more recent rule (80/20), not the older 100/0 default");
    }

    @Test
    void getEffectiveRule_returnsNull_whenNoRulesExist() {
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of());

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "ALL_DEPENDENTS", TODAY);

        assertNull(found);
    }

    @Test
    void applyCostSharing_returnsCorrectSplit_forSelfRuleAppliedToAllDependentsLookup() {
        // End-to-end check: HR set SELF=80/20, employee has spouse+child (resolver returns
        // ALL_DEPENDENTS). The applied split must be 80/20 of a ₹10,000 premium.
        when(cacheService.getRulesForCompany(COMPANY))
                .thenReturn(List.of(rule("GMC", CoverageCategory.SELF, 80, TODAY.minusDays(1))));

        CostShareSplit split = service.applyCostSharing(COMPANY, "GMC", "ALL_DEPENDENTS", BigDecimal.valueOf(10_000));

        assertEquals(0, BigDecimal.valueOf(8_000).compareTo(split.getEmployerShare()));
        assertEquals(0, BigDecimal.valueOf(2_000).compareTo(split.getEmployeeShare()));
    }

    @Test
    void applyCostSharing_defaultsTo100PercentEmployer_whenNoApplicableRule() {
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of());

        CostShareSplit split = service.applyCostSharing(COMPANY, "GMC", "ALL_DEPENDENTS", BigDecimal.valueOf(10_000));

        assertEquals(0, BigDecimal.valueOf(10_000).compareTo(split.getEmployerShare()));
        assertEquals(0, BigDecimal.ZERO.compareTo(split.getEmployeeShare()));
    }

    // -------------------------------------------------------------------------------------
    // DEFAULT category — the new explicit catch-all (Tier 1 design improvement).
    // -------------------------------------------------------------------------------------

    @Test
    void getEffectiveRule_defaultRule_appliesToAnyLookupCategory() {
        // HR adopts the new DEFAULT catch-all. A single rule should serve every family shape
        // unless a more specific category overrides it.
        when(cacheService.getRulesForCompany(COMPANY))
                .thenReturn(List.of(rule("GMC", CoverageCategory.DEFAULT, 75, TODAY.minusDays(1))));

        for (String lookup : new String[] {"SELF", "ALL_DEPENDENTS", "PARENT", "PARENT_IN_LAW"}) {
            CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", lookup, TODAY);
            assertNotNull(found, "DEFAULT must apply to lookup " + lookup);
            assertEquals(CoverageCategory.DEFAULT, found.getCoverageCategory());
        }
    }

    @Test
    void getEffectiveRule_exactMatch_winsOverDefault() {
        // DEFAULT exists, but HR set an explicit ALL_DEPENDENTS override. Override wins.
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of(
                rule("GMC", CoverageCategory.DEFAULT, 75, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.ALL_DEPENDENTS, 60, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "ALL_DEPENDENTS", TODAY);
        assertEquals(CoverageCategory.ALL_DEPENDENTS, found.getCoverageCategory());
    }

    @Test
    void getEffectiveRule_defaultRule_winsOverLegacyFallbacks() {
        // DEFAULT must be checked BEFORE the legacy SPOUSE/CHILD/SELF fallbacks; otherwise
        // an HR org that adopts DEFAULT can still get bitten by the messy legacy chain.
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of(
                rule("GMC", CoverageCategory.DEFAULT, 75, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.SELF, 100, TODAY.minusDays(1)),
                rule("GMC", CoverageCategory.SPOUSE, 80, TODAY.minusDays(1))));

        CostSharingRule found = service.getEffectiveRule(COMPANY, "GMC", "ALL_DEPENDENTS", TODAY);
        assertEquals(CoverageCategory.DEFAULT, found.getCoverageCategory());
        assertEquals(0, BigDecimal.valueOf(75).compareTo(found.getEmployerShareValue()));
    }

    // -------------------------------------------------------------------------------------
    // Surfacing applied rule metadata back to the caller.
    // -------------------------------------------------------------------------------------

    @Test
    void applyCostSharing_returnsRuleIdAndAppliedCategory_forDebugging() {
        CostSharingRule selfRule = rule("GMC", CoverageCategory.SELF, 80, TODAY.minusDays(1));
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of(selfRule));

        // ALL_DEPENDENTS lookup falls through to the SELF rule.
        CostShareSplit split = service.applyCostSharing(COMPANY, "GMC", "ALL_DEPENDENTS", BigDecimal.valueOf(10_000));

        assertEquals(selfRule.getId(), split.getRuleId());
        assertEquals(CoverageCategory.SELF, split.getAppliedCategory(),
                "applied category must reflect the rule that fired, not the resolver category");
    }

    @Test
    void applyCostSharing_appliedCategoryIsNull_whenNoRuleMatches() {
        when(cacheService.getRulesForCompany(COMPANY)).thenReturn(List.of());

        CostShareSplit split = service.applyCostSharing(COMPANY, "GMC", "SELF", BigDecimal.valueOf(10_000));

        assertNull(split.getRuleId());
        assertNull(split.getAppliedCategory());
    }
}
