package com.vimainsurance.vimaadmin.service.policy;

import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyMemberMappingHelperTest {

    @Test
    void membersForGmcFloater_excludesParentAndInLaw() {
        List<IPremiumCalculationService.MemberInfo> members = List.of(
                new IPremiumCalculationService.MemberInfo("EMPLOYEE", 35, LocalDate.of(1990, 1, 1)),
                new IPremiumCalculationService.MemberInfo("SPOUSE", 33, LocalDate.of(1992, 1, 1)),
                new IPremiumCalculationService.MemberInfo("parent", 62, LocalDate.of(1963, 1, 1)),
                new IPremiumCalculationService.MemberInfo("parent_in_law", 60, LocalDate.of(1965, 1, 1))
        );
        List<IPremiumCalculationService.MemberInfo> floater = PolicyMemberMappingHelper.membersForGmcFloater(members);
        assertEquals(2, floater.size());
        assertTrue(floater.stream().anyMatch(m -> "EMPLOYEE".equalsIgnoreCase(m.memberType())));
        assertTrue(floater.stream().anyMatch(m -> "SPOUSE".equalsIgnoreCase(m.memberType())));
    }

    @Test
    void membersForGmcFloater_emptyInput_returnsEmpty() {
        assertTrue(PolicyMemberMappingHelper.membersForGmcFloater(null).isEmpty());
        assertTrue(PolicyMemberMappingHelper.membersForGmcFloater(List.of()).isEmpty());
    }
}
