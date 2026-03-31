package com.vimainsurance.vimaadmin.service.policy;

import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes per-product-type rules for which insured members apply to GMC floater vs employee-only vs parent cover.
 */
public final class PolicyMemberMappingHelper {

    private PolicyMemberMappingHelper() {
    }

    /**
     * GMC/GHI floater: employee + spouse + children; excludes parent/in-law (those use PARENT_GMC only).
     */
    public static List<IPremiumCalculationService.MemberInfo> membersForGmcFloater(
            List<IPremiumCalculationService.MemberInfo> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<IPremiumCalculationService.MemberInfo> out = new ArrayList<>();
        for (IPremiumCalculationService.MemberInfo m : members) {
            if (m == null) continue;
            String mt = m.memberType() != null ? m.memberType().toLowerCase() : "";
            if ("parent".equals(mt) || "parent_in_law".equals(mt)) {
                continue;
            }
            out.add(m);
        }
        return out;
    }
}
