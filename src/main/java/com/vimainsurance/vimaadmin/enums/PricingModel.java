package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Premium pricing model for rate tables.
 */
public enum PricingModel {
    FLAT("FLAT"),
    AGE_BANDED("AGE_BANDED"),
    FAMILY_FLOATER("FAMILY_FLOATER");

    private final String value;

    PricingModel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PricingModel fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PricingModel model : PricingModel.values()) {
            if (model.value.equalsIgnoreCase(value)) {
                return model;
            }
        }
        throw new IllegalArgumentException("Invalid PricingModel: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
