package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ClaimStatus {
    DRAFT("DRAFT"),
    PENDING_REVIEW("PENDING_REVIEW"),
    INFO_REQUESTED("INFO_REQUESTED"),
    APPROVED_FOR_SUBMISSION("APPROVED_FOR_SUBMISSION"),
    SUBMITTED_TO_INSURER("SUBMITTED_TO_INSURER"),
    SUBMISSION_FAILED("SUBMISSION_FAILED"),
    INTIMATION_REJECTED("INTIMATION_REJECTED"),
    IN_PROGRESS("IN_PROGRESS"),
    QUERY_RAISED("QUERY_RAISED"),
    QUERY_RESPONDED("QUERY_RESPONDED"),
    APPROVED("APPROVED"),
    PAYMENT_PENDING("PAYMENT_PENDING"),
    SETTLED("SETTLED"),
    REJECTED("REJECTED"),
    REJECTED_BY_ADMIN("REJECTED_BY_ADMIN"),
    CLOSED("CLOSED");

    private final String value;

    ClaimStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClaimStatus fromValue(String value) {
        if (value == null || value.isBlank()) return null;
        for (ClaimStatus s : ClaimStatus.values()) {
            if (s.value.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown ClaimStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class ClaimStatusConverter implements AttributeConverter<ClaimStatus, String> {
        @Override
        public String convertToDatabaseColumn(ClaimStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }
        @Override
        public ClaimStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? ClaimStatus.fromValue(dbData) : null;
        }
    }
}
