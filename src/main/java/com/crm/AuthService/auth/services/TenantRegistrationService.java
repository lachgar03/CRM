package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.exception.EmailAlreadyExistsException;
import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.exception.TenantAlreadyExistsException;

public interface TenantRegistrationService {
    AuthResponse registerTenant(TenantRegistrationRequest request) throws TenantAlreadyExistsException, EmailAlreadyExistsException, RoleNotFoundException;

}
