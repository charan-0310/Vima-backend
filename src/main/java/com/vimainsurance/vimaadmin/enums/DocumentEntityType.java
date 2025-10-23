package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Document Entity Type Enum
 * 
 * Database Schema:
 * CREATE TYPE document.document_entity_type_enum AS ENUM (
 *     'lead', 'individual', 'policy', 'claim', 'organization'
 * );
 */
public enum DocumentEntityType {
    LEAD("LEAD"),
    INDIVIDUAL("INDIVIDUAL"),
    POLICY("POLICY"),
    CLAIM("CLAIM"),
    ORGANIZATION("ORGANIZATION"),   
    CUSTOMER("CUSTOMER");


    private final String value;

    DocumentEntityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DocumentEntityType fromValue(String value) {
        for (DocumentEntityType type : DocumentEntityType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentEntityType: " + value);
    }

    @Converter(autoApply = true)
    public static class DocumentEntityTypeConverter implements AttributeConverter<DocumentEntityType, String> {
        @Override
        public String convertToDatabaseColumn(DocumentEntityType type) {
            return type != null ? type.getValue() : null;
        }

        @Override
        public DocumentEntityType convertToEntityAttribute(String value) {
            return value != null ? DocumentEntityType.fromValue(value) : null;
        }
    }
}
