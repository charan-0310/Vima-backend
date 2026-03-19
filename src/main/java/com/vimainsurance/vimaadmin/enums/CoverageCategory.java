package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Coverage category for cost-sharing rules (self, spouse, child, parent, etc.).
 */
public enum CoverageCategory {
    SELF("SELF"),
    SPOUSE("SPOUSE"),
    CHILD("CHILD"),
    PARENT("PARENT"),
    PARENT_IN_LAW("PARENT_IN_LAW"),
    ALL_DEPENDENTS("ALL_DEPENDENTS");

    private final String value;

    CoverageCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CoverageCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (CoverageCategory c : CoverageCategory.values()) {
            if (c.value.equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Invalid CoverageCategory: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
