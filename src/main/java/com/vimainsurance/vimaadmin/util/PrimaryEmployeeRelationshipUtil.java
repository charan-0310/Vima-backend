package com.vimainsurance.vimaadmin.util;

import com.vimainsurance.vimaadmin.entity.Deals;

/**
 * Primary (insured employee) rows can be stored as {@code relationship = SELF} (endorsement / CSV)
 * or {@code relationship = EMPLOYEE} with {@code actualRelationship = SELF} (enrollment windows, demo seed).
 */
public final class PrimaryEmployeeRelationshipUtil {

    private PrimaryEmployeeRelationshipUtil() {
    }

    public static boolean isPrimarySelfEmployee(Deals d) {
        if (d == null) {
            return false;
        }
        String r = d.getRelationship();
        if (r == null || r.isBlank()) {
            return false;
        }
        String rel = r.trim();
        if ("SELF".equalsIgnoreCase(rel)) {
            return true;
        }
        if ("EMPLOYEE".equalsIgnoreCase(rel)) {
            String ar = d.getActualRelationship();
            return ar != null && "SELF".equalsIgnoreCase(ar.trim());
        }
        return false;
    }
}
