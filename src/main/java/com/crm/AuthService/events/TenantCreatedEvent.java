package com.crm.AuthService.events;

import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.tenant.entities.Tenant;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new Tenant record is created (status: PROVISIONING).
 * This triggers the asynchronous provisioning process.
 */
@Getter
public class TenantCreatedEvent extends ApplicationEvent {

    private final Tenant tenant;
    private final TenantRegistrationRequest registrationRequest;

    public TenantCreatedEvent(Tenant tenant, TenantRegistrationRequest registrationRequest) {
        super(tenant);
        this.tenant = tenant;
        this.registrationRequest = registrationRequest;
    }
}