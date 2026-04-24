package com.vimainsurance.vimaadmin.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.vimainsurance.vimaadmin.dto.ResponseDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps thrown exceptions to HTTP status + {@link ResponseDto} so the API does not
 * return Spring Boot’s default {@code {timestamp,status,error,path}} body without a clear cause.
 * <p>
 * The {@link #handleAny(Exception)} fallback runs last; check server logs for the full stack.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
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

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseDto<Void>> handleBadRequest(BadRequestException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(400, ex.getMessage()));
    }

    /** Multipart: missing required part (e.g. employees, file) — avoid generic 500. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ResponseDto<Void>> handleMissingRequestPart(MissingServletRequestPartException ex) {
        log.debug("Missing multipart part: {}", ex.getRequestPartName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(400, "Required part missing: " + ex.getRequestPartName()));
    }

    /** Invalid or unreadable JSON in a request body or multipart @RequestPart (e.g. employees array). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Unprocessable request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(400, "Invalid or malformed request data"));
    }

    /**
     * Multipart JSON part sent as application/octet-stream (common with FormData + Blob) used to break
     * @RequestPart object binding and surface as 500. Handled defensively; prefer manual JSON parse in controllers.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResponseDto<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.debug("Unsupported media type: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(400, "Unsupported content type for a request part. JSON parts should use application/json, or the server will read raw bytes where supported."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseDto<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ResponseDto<>(413, "File or request is too large. Reduce size or increase server limits."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ResponseDto<Void>> handleMultipart(MultipartException ex) {
        log.warn("Multipart error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(400, "Could not process multipart request: " + ex.getMessage()));
    }

    /** e.g. missing required {@code file} or {@code uploadType} on {@code POST .../upload}. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseDto<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "Required parameter is missing: '" + ex.getParameterName() + "'. This endpoint expects "
                + "multipart/form-data with: file, uploadType, one or more policyIds, and employees (JSON array part).";
        log.debug("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDto<>(400, msg));
    }

    /** e.g. policy id not a number, or wrong path variable type. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseDto<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName() != null ? ex.getName() : "parameter";
        Class<?> required = ex.getRequiredType();
        String typeHint = required != null ? " (expected " + required.getSimpleName() + ")" : "";
        String msg = "Invalid value for '" + name + "': " + (ex.getValue() != null ? ex.getValue() : "null")
                + typeHint;
        log.debug("Type mismatch: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDto<>(400, msg));
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ResponseDto<Void>> handleNotWritable(HttpMessageNotWritableException ex) {
        log.error("Response serialization failed (e.g. Jackson, circular reference): {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto<>(500, "Could not build JSON response. Check server logs. Cause: " + ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseDto<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseDto<>(404, "No static resource: " + ex.getResourcePath()));
    }

    /** {@code @PreAuthorize} or method security — avoid opaque 500 from security layer. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseDto<Void>> handleAccessDenied(AccessDeniedException ex) {
        String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? "Access denied: " + ex.getMessage()
                : "Access denied: you do not have permission for this action.";
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDto<>(403, msg));
    }

    /**
     * Last-resort: any unhandled {@link Exception} becomes a 500 with {@link ResponseDto}
     * and a message derived from the exception, plus full logging for operators.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Void>> handleAny(Exception ex) {
        log.error("Unhandled exception: {} — {}", ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto<>(500, buildUserFacingMessage(ex)));
    }

    private static String buildUserFacingMessage(Exception ex) {
        String simple = ex.getClass().getSimpleName();
        String m = ex.getMessage();
        if (m == null || m.isBlank()) {
            return "Unexpected error (" + simple + "). See server logs for the stack trace.";
        }
        if (m.length() > 800) {
            m = m.substring(0, 800) + "…";
        }
        return simple + ": " + m;
    }
}
