package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How employer share is expressed: percentage of total or fixed amount.
 */
public enum EmployerShareType {
    PERCENTAGE("PERCENTAGE"),
    FIXED_AMOUNT("FIXED_AMOUNT");

    private final String value;

    EmployerShareType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EmployerShareType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (EmployerShareType t : EmployerShareType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid EmployerShareType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
