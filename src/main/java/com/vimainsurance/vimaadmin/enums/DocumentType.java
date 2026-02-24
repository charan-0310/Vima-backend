package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Document Type Enum
 * 
 * Database Schema:
 * CREATE TYPE document.document_type_enum AS ENUM (
 *     'pan_card', 'aadhaar_card', 'passport', 'driving_license', 'voter_id',
 *     'bank_statement', 'income_proof', 'address_proof', 'photo',
 *     'policy_certificate', 'policy_schedule', 'endorsement', 'renewal_notice',
 *     'medical_bill', 'discharge_summary', 'prescription', 'lab_report',
 *     'diagnostic_report', 'claim_form', 'other'
 * );
 */
public enum DocumentType {
    PAN_CARD("PAN_CARD"),
    AADHAAR_CARD("AADHAAR_CARD"),
    PASSPORT("PASSPORT"),
    DRIVING_LICENSE("DRIVING_LICENSE"),
    VOTER_ID("VOTER_ID"),
    BANK_STATEMENT("BANK_STATEMENT"),
    INCOME_PROOF("INCOME_PROOF"),
    ADDRESS_PROOF("ADDRESS_PROOF"),
    PHOTO("PHOTO"),
    POLICY_CERTIFICATE("POLICY_CERTIFICATE"),
    POLICY_SCHEDULE("POLICY_SCHEDULE"),
    ENDORSEMENT("ENDORSEMENT"),
    SELF_ENROLLMENT("SELF_ENROLLMENT"),
    CLAIM_LETTER_APPROVAL("CLAIM_LETTER_APPROVAL"),
    CLAIM_LETTER_REJECTION("CLAIM_LETTER_REJECTION"),
    CLAIM_LETTER_QUERY("CLAIM_LETTER_QUERY"),
    CLAIM_LETTER_PAID("CLAIM_LETTER_PAID"),
    QUERY_RESPONSE_DOC("QUERY_RESPONSE_DOC"),
    RENEWAL_NOTICE("RENEWAL_NOTICE"),
    MEDICAL_BILL("MEDICAL_BILL"),
    DISCHARGE_SUMMARY("DISCHARGE_SUMMARY"),
    PRESCRIPTION("PRESCRIPTION"),
    LAB_REPORT("LAB_REPORT"),
    DIAGNOSTIC_REPORT("DIAGNOSTIC_REPORT"),
    CLAIM_FORM("CLAIM_FORM"),
    OTHER("OTHER"),
    POLICY("POLICY");

    private final String value;

    DocumentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DocumentType fromValue(String value) {
        for (DocumentType type : DocumentType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentType: " + value);
    }

    @Converter(autoApply = true)
    public static class DocumentTypeConverter implements AttributeConverter<DocumentType, String> {
        @Override
        public String convertToDatabaseColumn(DocumentType documentType) {
            return documentType != null ? documentType.getValue() : null;
        }

        @Override
        public DocumentType convertToEntityAttribute(String value) {
            return value != null ? DocumentType.fromValue(value) : null;
        }
    }
}
