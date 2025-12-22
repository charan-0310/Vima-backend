package com.vimainsurance.vimaadmin.exception;

/**
 * Exception thrown when document upload operations fail
 */
public class DocumentUploadException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public DocumentUploadException(String message) {
        super(message);
    }

    public DocumentUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}

