package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for coverage types in insurance policies
 */
public enum CoverageType {
    INDIVIDUAL("INDIVIDUAL"),
    FAMILY_FLOATER("FAMILY_FLOATER"),
    GROUP("GROUP");

    private final String value;

    CoverageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CoverageType fromValue(String value) {
        for (CoverageType type : CoverageType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CoverageType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
