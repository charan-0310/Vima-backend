package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum QueryStatus {
    OPEN("OPEN"),
    RESPONDED("RESPONDED"),
    CLOSED("CLOSED");

    private final String value;

    QueryStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static QueryStatus fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (QueryStatus s : QueryStatus.values()) {
            if (s.value.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown QueryStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class QueryStatusConverter implements AttributeConverter<QueryStatus, String> {
        @Override
        public String convertToDatabaseColumn(QueryStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public QueryStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? QueryStatus.fromValue(dbData) : null;
        }
    }
}
