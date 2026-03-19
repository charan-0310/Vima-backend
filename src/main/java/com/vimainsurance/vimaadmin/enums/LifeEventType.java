package com.vimainsurance.vimaadmin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Life event type for endorsements (marriage, birth, adoption, divorce, death).
 */
public enum LifeEventType {
    MARRIAGE("MARRIAGE"),
    BIRTH("BIRTH"),
    ADOPTION("ADOPTION"),
    DIVORCE("DIVORCE"),
    DEATH("DEATH");

    private final String value;

    LifeEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LifeEventType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (LifeEventType t : LifeEventType.values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid LifeEventType: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
