package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Payroll deduction frequency: monthly, quarterly, yearly.
 */
public enum DeductionFrequency {
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    YEARLY("YEARLY");

    private final String value;

    DeductionFrequency(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DeductionFrequency fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DeductionFrequency f : DeductionFrequency.values()) {
            if (f.value.equalsIgnoreCase(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Invalid DeductionFrequency: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
