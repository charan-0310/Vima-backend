package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum EndorsementSource {
    CSV_UPLOAD("CSV_UPLOAD"),
    SELF_ENROLLMENT("SELF_ENROLLMENT"),
    API("API"),
    MANUAL("MANUAL"),
    HRMS_SYNC("HRMS_SYNC");

    private final String value;

    EndorsementSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EndorsementSource fromValue(String value) {
        for (EndorsementSource src : EndorsementSource.values()) {
            if (src.value.equals(value)) {
                return src;
            }
        }
        throw new IllegalArgumentException("Unknown EndorsementSource: " + value);
    }

    @Converter(autoApply = true)
    public static class EndorsementSourceConverter implements AttributeConverter<EndorsementSource, String> {
        @Override
        public String convertToDatabaseColumn(EndorsementSource src) {
            return src != null ? src.getValue() : null;
        }

        @Override
        public EndorsementSource convertToEntityAttribute(String value) {
            return value != null ? EndorsementSource.fromValue(value) : null;
        }
    }
}

