package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ClaimCategory {
    FRESH_CLAIM("FRESH_CLAIM"),
    FOLLOW_UP("FOLLOW_UP");

    private final String value;

    ClaimCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClaimCategory fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (ClaimCategory c : ClaimCategory.values()) {
            if (c.value.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown ClaimCategory: " + value);
    }

    @Converter(autoApply = true)
    public static class ClaimCategoryConverter implements AttributeConverter<ClaimCategory, String> {
        @Override
        public String convertToDatabaseColumn(ClaimCategory attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public ClaimCategory convertToEntityAttribute(String dbData) {
            return dbData != null ? ClaimCategory.fromValue(dbData) : null;
        }
    }
}
