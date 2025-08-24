package com.wms.billing.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Global exception handler that translates exceptions into consistent JSON responses.
 *
 * author Prasad Ganta
 * since 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                              HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.debug("Handled ResponseStatusException: {} {}", status.value(), ex.getReason());
        return build(status, ex.getReason(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest req) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(toList());
        log.debug("Validation failed: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest req) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(this::formatViolation)
                .collect(toList());
        log.debug("Constraint violations: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.debug("Malformed request body: {}", cause);
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", req, List.of(cause));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest req) {
        log.debug("Illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, nonNullMessage(ex), req, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex,
                                                            HttpServletRequest req) {
        log.debug("Illegal state: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, nonNullMessage(ex), req, null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex,
                                                       HttpServletRequest req) {
        log.error("Unhandled runtime exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", req, List.of(nonNullMessage(ex)));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleThrowable(Throwable ex,
                                                         HttpServletRequest req) {
        log.error("Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", req, List.of(nonNullMessage(ex)));
    }


    @ExceptionHandler(RateSheetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRateSheetNotFound(RateSheetNotFoundException ex,
                                                                 HttpServletRequest req) {
        log.debug("RateSheet not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, nonNullMessage(ex), req, null);
    }


    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                HttpServletRequest req,
                                                List<String> errors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                errors == null || errors.isEmpty() ? null : errors
        );
        return ResponseEntity.status(status).body(body);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest req) {
        String cause = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.debug("Data integrity violation: {}", cause);
        return build(HttpStatus.BAD_REQUEST, "Data integrity violation", req, java.util.List.of(cause));
    }


    private String formatFieldError(FieldError fe) {
        String field = fe.getField();
        String code = fe.getCode();
        String msg = fe.getDefaultMessage();
        return "%s: %s%s".formatted(field, msg != null ? msg : "invalid", code != null ? " (" + code + ")" : "");
    }

    private String formatViolation(ConstraintViolation<?> v) {
        String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "<unknown>";
        String msg = v.getMessage();
        return "%s: %s".formatted(path, msg);
    }

    private String nonNullMessage(Throwable t) {
        return Objects.requireNonNullElse(t.getMessage(), t.getClass().getSimpleName());
    }

    public static final class ErrorResponse {
        private final Instant timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;
        private final List<String> errors;

        public ErrorResponse(Instant timestamp, int status, String error, String message, String path, List<String> errors) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.errors = errors;
        }

        public Instant getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public List<String> getErrors() { return errors; }
    }
}