package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum EnrollementStatus {
    ACTIVE("ACTIVE"),
    SCHEDULED("SCHEDULED"),
    CLOSED("CLOSED"),
    CANCELLED("CANCELLED"),
    PENDING("PENDING"),
    SENT("SENT"),
    OPENED("OPENED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    EXPIRED("EXPIRED"),
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ENDORSED("ENDORSED"),
    PENDING_APPROVAL("PENDING_APPROVAL"),
    INACTIVE("INACTIVE");

    private final String value;

    EnrollementStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EnrollementStatus fromValue(String value) {
        for (EnrollementStatus status : EnrollementStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown EnrollementStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class EnrollementStatusConverter implements AttributeConverter<EnrollementStatus, String> {
        @Override
        public String convertToDatabaseColumn(EnrollementStatus status) {
            return status != null ? status.getValue() : null;
        }
        @Override
        public EnrollementStatus convertToEntityAttribute(String value) {
            return value != null ? EnrollementStatus.fromValue(value) : null;
        }
    }

}
