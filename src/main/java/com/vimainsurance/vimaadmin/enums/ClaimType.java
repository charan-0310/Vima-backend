package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ClaimType {
    REIMBURSEMENT("REIMBURSEMENT"),
    CASHLESS("CASHLESS");

    private final String value;

    ClaimType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClaimType fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (ClaimType t : ClaimType.values()) {
            if (t.value.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown ClaimType: " + value);
    }

    @Converter(autoApply = true)
    public static class ClaimTypeConverter implements AttributeConverter<ClaimType, String> {
        @Override
        public String convertToDatabaseColumn(ClaimType attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public ClaimType convertToEntityAttribute(String dbData) {
            return dbData != null ? ClaimType.fromValue(dbData) : null;
        }
    }
}
