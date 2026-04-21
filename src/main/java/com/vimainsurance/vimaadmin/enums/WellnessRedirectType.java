package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WellnessRedirectType {
    DIRECT("DIRECT"),
    BACKEND_TOKEN("BACKEND_TOKEN"),
    SSO_JWT("SSO_JWT");

    private final String value;

    WellnessRedirectType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WellnessRedirectType fromValue(String value) {
        for (WellnessRedirectType type : WellnessRedirectType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WellnessRedirectType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
