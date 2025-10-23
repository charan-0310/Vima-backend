package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for policy status in insurance policies
 */
public enum PolicyStatus {
    ACTIVE("ACTIVE"),
    LAPSED("LAPSED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED"),
    PENDING("PENDING");

    private final String value;

    PolicyStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PolicyStatus fromValue(String value) {
        for (PolicyStatus status : PolicyStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid PolicyStatus: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
