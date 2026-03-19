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
    ARTIFICIAL_INTELLIGENCE_AND_TECHNOLOGY("ARTIFICIAL_INTELLIGENCE_AND_TECHNOLOGY"),
    BANKING("BANKING");

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
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        // Accept common alias for ARTIFICIAL_INTELLIGENCE_AND_TECHNOLOGY
        if ("ARTIFICIAL_INTELLIGENCE_TECHNOLOGY".equalsIgnoreCase(normalized)) {
            return ARTIFICIAL_INTELLIGENCE_AND_TECHNOLOGY;
        }
        for (Industry industry : Industry.values()) {
            if (industry.value.equalsIgnoreCase(normalized)) {
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

