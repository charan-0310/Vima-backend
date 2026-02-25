package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum SubmissionSource {
    EMPLOYEE_PORTAL("EMPLOYEE_PORTAL"),
    ADMIN("ADMIN"),
    API_SYNC("API_SYNC");

    private final String value;

    SubmissionSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SubmissionSource fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (SubmissionSource s : SubmissionSource.values()) {
            if (s.value.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown SubmissionSource: " + value);
    }

    @Converter(autoApply = true)
    public static class SubmissionSourceConverter implements AttributeConverter<SubmissionSource, String> {
        @Override
        public String convertToDatabaseColumn(SubmissionSource attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public SubmissionSource convertToEntityAttribute(String dbData) {
            return dbData != null ? SubmissionSource.fromValue(dbData) : null;
        }
    }
}
