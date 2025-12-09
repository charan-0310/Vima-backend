package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum NomineeRelationship {
    SPOUSE("SPOUSE"),
    CHILD("CHILD"),
    FATHER("FATHER"),
    MOTHER("MOTHER"),
    PARENT("PARENT"),
    SIBLING("SIBLING"),
    OTHER("OTHER");

    private final String value;

    NomineeRelationship(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NomineeRelationship fromValue(String value) {
        for (NomineeRelationship relationship : NomineeRelationship.values()) {
            if (relationship.value.equalsIgnoreCase(value)) {
                return relationship;
            }
        }
        throw new IllegalArgumentException("Unknown NomineeRelationship: " + value);
    }

    @Converter(autoApply = true)
    public static class NomineeRelationshipConverter implements AttributeConverter<NomineeRelationship, String> {

        @Override
        public String convertToDatabaseColumn(NomineeRelationship relationship) {
            return relationship != null ? relationship.getValue() : null;
        }

        @Override
        public NomineeRelationship convertToEntityAttribute(String value) {
            return value != null ? NomineeRelationship.fromValue(value) : null;
        }
    }
}

