package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Document Category Enum
 * 
 * Database Schema:
 * CREATE TYPE document.document_category_enum AS ENUM (
 *     'kyc_documents', 'policy_documents', 'claim_documents',
 *     'financial_documents', 'medical_records', 'other'
 * );
 */
public enum DocumentCategory {
    KYC_DOCUMENTS("KYC_DOCUMENTS"),
    POLICY_DOCUMENTS("POLICY_DOCUMENTS"),
    CLAIM_DOCUMENTS("CLAIM_DOCUMENTS"),
    FINANCIAL_DOCUMENTS("FINANCIAL_DOCUMENTS"),
    MEDICAL_RECORDS("MEDICAL_RECORDS"),
    OTHER("OTHER");

    private final String value;

    DocumentCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DocumentCategory fromValue(String value) {
        for (DocumentCategory category : DocumentCategory.values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentCategory: " + value);
    }

    @Converter(autoApply = true)
    public static class DocumentCategoryConverter implements AttributeConverter<DocumentCategory, String> {
        @Override
        public String convertToDatabaseColumn(DocumentCategory category) {
            return category != null ? category.getValue() : null;
        }

        @Override
        public DocumentCategory convertToEntityAttribute(String value) {
            return value != null ? DocumentCategory.fromValue(value) : null;
        }
    }
}
