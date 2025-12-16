package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum EndorsementType {
    ADDITION("ADDITION"),
    DELETION("DELETION");

    private final String value;

    EndorsementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EndorsementType fromValue(String value) {
        for (EndorsementType type : EndorsementType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EndorsementType: " + value);
    }

    @Converter(autoApply = true)
    public static class EndorsementTypeConverter implements AttributeConverter<EndorsementType, String> {
        @Override
        public String convertToDatabaseColumn(EndorsementType type) {
            return type != null ? type.getValue() : null;
        }

        @Override
        public EndorsementType convertToEntityAttribute(String value) {
            return value != null ? EndorsementType.fromValue(value) : null;
        }
    }
}

