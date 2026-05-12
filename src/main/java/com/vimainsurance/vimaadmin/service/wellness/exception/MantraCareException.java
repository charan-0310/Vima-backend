package com.vimainsurance.vimaadmin.service.wellness.exception;

/** MantraCare API or response error (maps to HTTP 502). */
public class MantraCareException extends RuntimeException {

    public MantraCareException(String message) {
        super(message);
    }

    public MantraCareException(String message, Throwable cause) {
        super(message, cause);
    }
}
