package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for product types in insurance policies
 */
public enum ProductType {
    HEALTH("HEALTH");

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
}
