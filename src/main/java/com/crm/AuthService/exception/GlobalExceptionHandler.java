package com.crm.AuthService.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for the application.
 * Intercepts exceptions and returns standardized JSON error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Helper to create a standard error response body
    private Map<String, Object> createErrorBody(String error, String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    // --- Authentication & Authorization Exceptions ---

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> body = createErrorBody("Unauthorized", "Invalid email or password", HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = createErrorBody("Forbidden", ex.getMessage(), HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleInvalidToken(InvalidTokenException ex) {
        Map<String, Object> body = createErrorBody("Unauthorized", ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<Object> handleAccountStatus(RuntimeException ex) {
        Map<String, Object> body = createErrorBody("Unauthorized", ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    // --- Business Logic: Not Found Exceptions (404) ---

    @ExceptionHandler({UserNotFoundException.class, RoleNotFoundException.class, TenantNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex) {
        Map<String, Object> body = createErrorBody("Not Found", ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // --- Business Logic: Conflict Exceptions (409) ---

    @ExceptionHandler({EmailAlreadyExistsException.class, TenantAlreadyExistsException.class})
    public ResponseEntity<Object> handleConflict(RuntimeException ex) {
        Map<String, Object> body = createErrorBody("Conflict", ex.getMessage(), HttpStatus.CONFLICT);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // --- Business Logic: Provisioning/Server Error (500) ---

    @ExceptionHandler(TenantProvisioningException.class)
    public ResponseEntity<Object> handleProvisioningFailure(TenantProvisioningException ex, WebRequest request) {
        log.error("Tenant provisioning failed: {}", ex.getMessage(), ex);
        Map<String, Object> body = createErrorBody("Internal Server Error", "Tenant provisioning failed: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- Validation Exception (400) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        Map<String, Object> body = createErrorBody("Bad Request", "Validation failed: " + errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // --- Generic Fallback Exception (500) ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        Map<String, Object> body = createErrorBody("Internal Server Error", "An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}