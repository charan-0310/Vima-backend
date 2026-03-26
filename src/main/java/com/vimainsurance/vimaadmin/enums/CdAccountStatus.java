package com.vimainsurance.vimaadmin.enums;

public enum CdAccountStatus {
    ACTIVE,
    INACTIVE;

    public static CdAccountStatus fromValue(String value) {
        for (CdAccountStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CdAccountStatus: " + value);
    }
}
