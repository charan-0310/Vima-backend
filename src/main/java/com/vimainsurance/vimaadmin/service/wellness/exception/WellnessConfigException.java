package com.vimainsurance.vimaadmin.service.wellness.exception;

/** Client or configuration error for wellness redirect (maps to HTTP 400). */
public class WellnessConfigException extends RuntimeException {

    public WellnessConfigException(String message) {
        super(message);
    }
}
