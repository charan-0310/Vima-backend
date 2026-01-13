package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for organization industry. Must mirror DB enum cpc.industry_enum
 */
public enum Industry {
    IT_SERVICES("IT_SERVICES"),
    BANKING_FINANCE("BANKING_FINANCE"),
    MANUFACTURING("MANUFACTURING"),
    HEALTHCARE("HEALTHCARE"),
    EDUCATION("EDUCATION"),
    RETAIL("RETAIL"),
    TELECOMMUNICATIONS("TELECOMMUNICATIONS"),
    PHARMACEUTICALS("PHARMACEUTICALS"),
    AUTOMOTIVE("AUTOMOTIVE"),
    REAL_ESTATE("REAL_ESTATE"),
    OTHER("OTHER"),
    ARTIFICIAL_INTELLIGENCE_TECHNOLOGY("ARTIFICIAL_INTELLIGENCE_/_TECHNOLOGY");

    private final String value;

    Industry(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Industry fromValue(String value) {
        for (Industry industry : Industry.values()) {
            if (industry.value.equalsIgnoreCase(value)) {
                return industry;
            }
        }
        throw new IllegalArgumentException("Invalid Industry: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

