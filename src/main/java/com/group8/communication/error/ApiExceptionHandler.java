package com.group8.communication.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException exception) {
        String code = exception.getReason() == null ? "REQUEST_FAILED" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(code));
    }

    /** @PreAuthorize denials (e.g. a student creating an announcement) are 403, not 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("FORBIDDEN"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation() {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("INTERNAL_SERVER_ERROR"));
    }
}
