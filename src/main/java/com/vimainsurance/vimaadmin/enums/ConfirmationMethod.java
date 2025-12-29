package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ConfirmationMethod {
    EMAIL("EMAIL"),
    PORTAL("PORTAL"),
    API("API"),
    PHONE_CALL("PHONE_CALL"),
    IN_PERSON("IN_PERSON"),
    OTHER("OTHER");

    private final String value;

    ConfirmationMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConfirmationMethod fromValue(String value) {
        for (ConfirmationMethod confirmationMethod : ConfirmationMethod.values()) {
            if (confirmationMethod.value.equals(value)) {
                return confirmationMethod;
            }
        }
        throw new IllegalArgumentException("Unknown ConfirmationMethod: " + value);
    }
    @Converter(autoApply = true)
    public static class ConfirmationMethodConverter implements AttributeConverter<ConfirmationMethod, String> {
        @Override
        public String convertToDatabaseColumn(ConfirmationMethod method) {
            return method != null ? method.getValue() : null;
        }

        @Override
        public ConfirmationMethod convertToEntityAttribute(String value) {
            return value != null ? ConfirmationMethod.fromValue(value) : null;
        }
    }
}

