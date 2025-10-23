package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Document Verification Status Enum
 * 
 * Database Schema:
 * CREATE TYPE document.document_verification_status_enum AS ENUM (
 *     'pending', 'verified', 'rejected'
 * );
 */
public enum DocumentVerificationStatus {
    PENDING("PENDING"),
    VERIFIED("VERIFIED"),
    REJECTED("REJECTED");

    private final String value;

    DocumentVerificationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DocumentVerificationStatus fromValue(String value) {
        for (DocumentVerificationStatus status : DocumentVerificationStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentVerificationStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class DocumentVerificationStatusConverter implements AttributeConverter<DocumentVerificationStatus, String> {
        @Override
        public String convertToDatabaseColumn(DocumentVerificationStatus status) {
            return status != null ? status.getValue() : null;
        }

        @Override
        public DocumentVerificationStatus convertToEntityAttribute(String value) {
            return value != null ? DocumentVerificationStatus.fromValue(value) : null;
        }
    }
}
