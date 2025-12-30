package com.vimainsurance.vimaadmin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


public enum AccountStatus {
INACTIVE("INACTIVE"),
ACTIVE("ACTIVE"),
SUSPENDED("SUSPENDED"),
PENDING("PENDING"),
PENDING_DELETE("PENDING_DELETE"),
PENDING_APPROVAL("PENDING_APPROVAL"),
APPROVED("APPROVED"),
REJECTED("REJECTED"),
COMPLETED("COMPLETED"),
PENDING_EXIT("PENDING_EXIT"),
LEAVING("LEAVING");

private final String value;

AccountStatus(String value) {
    this.value = value;
}

public String getValue() {
    return value;
}

public static AccountStatus fromValue(String value) {
    for (AccountStatus accountStatus : AccountStatus.values()) {
        if (accountStatus.value.equals(value)) {
            return accountStatus;
        }
    }
    throw new IllegalArgumentException("Unknown AccountStatus: " + value);
}

@Converter(autoApply = true)
public static class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {
    @Override
    public String convertToDatabaseColumn(AccountStatus accountStatus) {
        return accountStatus != null ? accountStatus.getValue() : null;
    }

    @Override
    public AccountStatus convertToEntityAttribute(String value) {
        return value != null ? AccountStatus.fromValue(value) : null;
    }
}
}
