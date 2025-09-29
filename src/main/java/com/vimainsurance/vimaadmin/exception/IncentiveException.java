package com.vimainsurance.vimaadmin.exception;

public class IncentiveException extends RuntimeException {
    public IncentiveException(String message) {
        super(message);
    }

    public IncentiveException(String message, Throwable cause) {
        super(message, cause);
    }
} 