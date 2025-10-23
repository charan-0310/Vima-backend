package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


public enum AccountType {
RETAIL_PRIMARY("RETAIL_PRIMARY"),
RETAIL_DEPENDENT("RETAIL_DEPENDENT"),
CORPORATE_EMPLOYEE("CORPORATE_EMPLOYEE"),
CORPORATE_DEPENDENT("CORPORATE_DEPENDENT");

private final String value;

AccountType(String value) {
    this.value = value;
}

public String getValue() {
    return value;
}

public static AccountType fromValue(String value) {
    for (AccountType accountType : AccountType.values()) {
        if (accountType.value.equals(value)) {
            return accountType;
        }
    }
    throw new IllegalArgumentException("Unknown AccountType: " + value);
}

@Converter(autoApply = true)
public static class AccountTypeConverter implements AttributeConverter<AccountType, String> {
    @Override
    public String convertToDatabaseColumn(AccountType accountType) {
        return accountType != null ? accountType.getValue() : null;
    }

    @Override
    public AccountType convertToEntityAttribute(String value) {
        return value != null ? AccountType.fromValue(value) : null;
    }
}
}
