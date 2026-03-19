package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for coverage types in insurance policies
 * Includes traditional coverage types and GMC-specific coverage types
 */
public enum CoverageType {
    // Traditional Coverage Types
    INDIVIDUAL("INDIVIDUAL"),
    FAMILY_FLOATER("FAMILY_FLOATER"),
    GROUP("GROUP"),

    // GMC Coverage Types
    E("E"),           // Employee only
    ES("ES"),         // Employee + Spouse
    ESC("ESC"),       // Employee + Spouse + Children
    ESCP("ESCP"),     // Employee + Spouse + Children + Parents
    PARENT("PARENT"); // Parents only (add-on, requires active ESC policy)

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

    /**
     * Check if this is a GMC coverage type
     */
    public boolean isGMCCoverageType() {
        return this == E || this == ES || this == ESC || this == ESCP;
    }

    /**
     * Get description of coverage type
     */
    public String getDescription() {
        return switch (this) {
            case E -> "Employee Only";
            case ES -> "Employee + Spouse";
            case ESC -> "Employee + Spouse + Children";
            case ESCP -> "Employee + Spouse + Children + Parents";
            case PARENT -> "Parents Only (add-on)";
            case INDIVIDUAL -> "Individual Coverage";
            case FAMILY_FLOATER -> "Family Floater";
            case GROUP -> "Group Coverage";
        };
    }
}
