package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WellnessCategory {
    TELEMEDICINE("TELEMEDICINE"),
    DIAGNOSTICS("DIAGNOSTICS"),
    FITNESS("FITNESS"),
    MENTAL_HEALTH("MENTAL_HEALTH"),
    NUTRITION("NUTRITION");

    private final String value;

    WellnessCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WellnessCategory fromValue(String value) {
        for (WellnessCategory category : WellnessCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid WellnessCategory: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
