package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Relationship {
    SELF("SELF"),
    SPOUSE("SPOUSE"),
    CHILD("CHILD"),
    PARENT("PARENT"),
    OTHER("OTHER");

    private final String value;

    Relationship(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Relationship fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (Relationship r : Relationship.values()) {
            if (r.value.equals(value)) return r;
        }
        throw new IllegalArgumentException("Unknown Relationship: " + value);
    }

    @Converter(autoApply = true)
    public static class RelationshipConverter implements AttributeConverter<Relationship, String> {
        @Override
        public String convertToDatabaseColumn(Relationship attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public Relationship convertToEntityAttribute(String dbData) {
            return dbData != null ? Relationship.fromValue(dbData) : null;
        }
    }
}
