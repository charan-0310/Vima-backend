package com.vimainsurance.vimaadmin.exception;

public class ZohoSyncException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public ZohoSyncException(String message) {
        super(message);
    }

    public ZohoSyncException(String message, Throwable cause) {
        super(message, cause);
    }
} 