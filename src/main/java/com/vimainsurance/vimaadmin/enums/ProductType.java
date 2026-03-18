package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for product types and policy types in insurance policies
 * Includes traditional product types and new policy types (GMC, GPA, GTL)
 */
public enum ProductType {
    // Traditional Product Types
    HEALTH("HEALTH"),
    MOTOR("MOTOR"),
    GENERAL("GENERAL"),
    TERM("TERM"),
    LIFE("LIFE"),
    GHI("GHI"),
    GTI("GTI"),

    // Policy Types (Employee Benefits)
    GMC("GMC"),    // Group Medical Coverage (Health Insurance with TPA)
    GPA("GPA"),    // Group Personal Accident
    GTL("GTL"),   // Group Term Life
    PARENT_GMC("PARENT_GMC"), // Parent/in-law coverage (Scenario 2 separate policy)
    TOP_UP("TOP_UP"),         // Top-up coverage (with deductible)
    SUPER_TOP_UP("SUPER_TOP_UP"), // Super top-up coverage (with deductible)

    EMPLOYEE("EMPLOYEE"),
    PROPERTY("PROPERTY"),
    LIABILITY("LIABILITY");

    private final String value;

    ProductType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductType fromValue(String value) {
        for (ProductType type : ProductType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ProductType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Check if this product type supports dependents
     */
    public boolean supportsDependents() {
        return this == GMC || this == GHI || this == HEALTH;
    }

    /**
     * Check if this product type requires TPA
     */
    public boolean requiresTPA() {
        return this == GMC;
    }

    /**
     * Check if this product type uses CTC multiplier
     */
    public boolean usesCTCMultiplier() {
        return this == GPA || this == GTL;
    }

    /**
     * Check if this is a policy type (not a product type)
     */
    public boolean isPolicyType() {
        return this == GMC || this == GPA || this == GTL || this == PARENT_GMC || this == TOP_UP || this == SUPER_TOP_UP;
    }
}
