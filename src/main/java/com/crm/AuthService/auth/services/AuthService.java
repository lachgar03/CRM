package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.LoginRequest;
import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.exception.*;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest) throws BadCredentialsException, DisabledException;
    AuthResponse refreshToken(String refreshToken) throws InvalidTokenException, UserNotFoundException;
    AuthResponse registerTenant(TenantRegistrationRequest request) throws TenantAlreadyExistsException,EmailAlreadyExistsException,RoleNotFoundException;
}
