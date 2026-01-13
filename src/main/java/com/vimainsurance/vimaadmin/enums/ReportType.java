package com.vimainsurance.vimaadmin.enums;

public enum ReportType {
    MASTER("MASTER"),
    ENROLLMENT("ENROLLMENT"),
    PAYROLL("PAYROLL");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReportType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ReportType value cannot be null");
        }
        for (ReportType type : ReportType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ReportType: " + value);
    }
}

