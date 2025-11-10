package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Gender {
    MALE("MALE"),
    FEMALE("FEMALE"),
    OTHER("OTHER");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Gender fromValue(String value) {
        for (Gender gender : Gender.values()) {
            if (gender.value.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown Gender: " + value);
    }

    @Converter(autoApply = true)
    public static class GenderConverter implements AttributeConverter<Gender, String> {

        @Override
        public String convertToDatabaseColumn(Gender gender) {
            return gender != null ? gender.getValue() : null;
        }

        @Override
        public Gender convertToEntityAttribute(String value) {
            return value != null ? Gender.fromValue(value) : null;
        }
    }
}

