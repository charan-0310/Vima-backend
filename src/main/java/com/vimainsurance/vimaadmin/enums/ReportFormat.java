package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Payroll deduction report output format.
 */
public enum ReportFormat {
    XLSX("xlsx"),
    CSV("csv");

    private final String value;

    ReportFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReportFormat fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ReportFormat f : ReportFormat.values()) {
            if (f.value.equalsIgnoreCase(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Invalid ReportFormat: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
