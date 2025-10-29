package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * User Role Enum
 * 
 * Database Schema:
 * CREATE TYPE admin.user_role_enum AS ENUM (
 *     'SUPER_ADMIN', 'ADMIN', 'SALES_AGENT', 'SALES_POSP',
 *     'CLAIMS_PROCESSOR', 'SUPPORT_AGENT', 'SALES_MANAGER'
 * );
 */
public enum UserRole {
    SUPER_ADMIN("SUPER_ADMIN"),
    ADMIN("ADMIN"),
    SALES_ADMIN("SALES_ADMIN"),
    SALES_AGENT("SALES_AGENT"),
    SALES_POSP("SALES_POSP"),
    CLAIMS_PROCESSOR("CLAIMS_PROCESSOR"),
    SUPPORT_AGENT("SUPPORT_AGENT"),
    SALES_MANAGER("SALES_MANAGER");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown UserRole: " + value);
    }

    @Converter(autoApply = true)
    public static class UserRoleConverter implements AttributeConverter<UserRole, String> {
        @Override
        public String convertToDatabaseColumn(UserRole role) {
            return role != null ? role.getValue() : null;
        }

        @Override
        public UserRole convertToEntityAttribute(String value) {
            return value != null ? UserRole.fromValue(value) : null;
        }
    }
}
