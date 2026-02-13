package com.vimainsurance.vimaadmin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to access an enrollment window 
 * that is not active (scheduled, closed, or cancelled).
 * Returns HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class EnrollmentWindowClosedException extends RuntimeException {
    
    public EnrollmentWindowClosedException(String message) {
        super(message);
    }
    
    public EnrollmentWindowClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
