package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PremiumChangeType {
    INCREASE("INCREASE"),
    DECREASE("DECREASE"),
    NO_CHANGE("NO_CHANGE");

    private final String value;

    PremiumChangeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PremiumChangeType fromValue(String value) {
        for (PremiumChangeType type : PremiumChangeType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PremiumChangeType: " + value);
    }

    @Converter(autoApply = true)
    public static class PremiumChangeTypeConverter implements AttributeConverter<PremiumChangeType, String> {
        @Override
        public String convertToDatabaseColumn(PremiumChangeType type) {
            return type != null ? type.getValue() : null;
        }

        @Override
        public PremiumChangeType convertToEntityAttribute(String value) {
            return value != null ? PremiumChangeType.fromValue(value) : null;
        }
    }
}

