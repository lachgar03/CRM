package com.crm.AuthService.exception;

public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String message) {
        super(message);
    }
}
