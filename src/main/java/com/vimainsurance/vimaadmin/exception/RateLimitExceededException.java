package com.vimainsurance.vimaadmin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when rate limit is exceeded for token validation.
 * Returns HTTP 429 Too Many Requests.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
