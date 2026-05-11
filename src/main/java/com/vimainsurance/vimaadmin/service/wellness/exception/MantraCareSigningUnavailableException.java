package com.vimainsurance.vimaadmin.service.wellness.exception;

/** Partner private key not configured or failed to load (maps to HTTP 503). */
public class MantraCareSigningUnavailableException extends RuntimeException {

    public MantraCareSigningUnavailableException(String message) {
        super(message);
    }
}
