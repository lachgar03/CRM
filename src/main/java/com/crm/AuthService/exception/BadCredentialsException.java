package com.crm.AuthService.exception;

public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
