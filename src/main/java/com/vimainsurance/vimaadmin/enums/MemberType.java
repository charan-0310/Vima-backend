package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum MemberType {
    EMPLOYEE("EMPLOYEE"),
    DEPENDENT("DEPENDENT");

    private final String value;

    MemberType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MemberType fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (MemberType t : MemberType.values()) {
            if (t.value.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown MemberType: " + value);
    }

    @Converter(autoApply = true)
    public static class MemberTypeConverter implements AttributeConverter<MemberType, String> {
        @Override
        public String convertToDatabaseColumn(MemberType attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public MemberType convertToEntityAttribute(String dbData) {
            return dbData != null ? MemberType.fromValue(dbData) : null;
        }
    }
}
