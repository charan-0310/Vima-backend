package com.vimainsurance.vimaadmin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to use an enrollment token 
 * that has already been completed.
 * Returns HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class TokenAlreadyUsedException extends RuntimeException {
    
    public TokenAlreadyUsedException(String message) {
        super(message);
    }
    
    public TokenAlreadyUsedException(String message, Throwable cause) {
        super(message, cause);
    }
}
