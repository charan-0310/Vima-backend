package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Source of the premium rate (insurer card vs negotiated).
 */
public enum RateSource {
    INSURER_CARD("INSURER_CARD"),
    NEGOTIATED("NEGOTIATED");

    private final String value;

    RateSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RateSource fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (RateSource source : RateSource.values()) {
            if (source.value.equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Invalid RateSource: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
