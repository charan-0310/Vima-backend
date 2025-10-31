package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for policy payment frequency. Must mirror DB enum cpc.payment_frequency_enum
 * (MONTHLY, QUARTERLY, BI_ANNUALLY, YEARLY).
 */
public enum PaymentFrequency {
    MONTHLY("MONTHLY"),   
    QUARTERLY("QUARTERLY"),
    BI_ANNUALLY("BI_ANNUALLY"),
    YEARLY("YEARLY");

    private final String value;

    PaymentFrequency(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PaymentFrequency fromValue(String value) {
        for (PaymentFrequency type : PaymentFrequency.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PaymentFrequency: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}


