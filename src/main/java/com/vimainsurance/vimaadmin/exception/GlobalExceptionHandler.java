package com.vimainsurance.vimaadmin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vimainsurance.vimaadmin.dto.ResponseDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Handles exceptions from controller layer so controllers and services can throw
 * without try-catch. Keeps security and validation errors out of controller code.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrganizationAccessDeniedException.class)
    public ResponseEntity<ResponseDto<Void>> handleOrganizationAccessDenied(OrganizationAccessDeniedException ex) {
        log.warn("Organization access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ResponseDto<>(403, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDto<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(400, ex.getMessage()));
    }
}
