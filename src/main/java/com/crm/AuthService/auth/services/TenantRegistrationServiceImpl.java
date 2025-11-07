package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.events.TenantCreatedEvent; // IMPORT EVENT
import com.crm.AuthService.exception.EmailAlreadyExistsException;
import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.exception.TenantAlreadyExistsException;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crm.AuthService.role.entities.Role;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationServiceImpl implements TenantRegistrationService {
    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher; // INJECT PUBLISHER

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String TENANT_STATUS_PROVISIONING = "PROVISIONING";



    @Override
    @Transactional // This transaction is now very short and fast
    public void registerTenant(TenantRegistrationRequest request) { // RETURN TYPE CHANGED
        log.info("========================================");
        log.info("Phase 1: Starting Tenant Registration (Synchronous)");
        log.info("Subdomain: {}", request.getSubdomain());
        log.info("Admin Email: {}", request.getAdminEmail());
        log.info("========================================");


        log.info("Phase 1: Pre-validation checks");

        authHelper.validateSubdomain(request.getSubdomain());
        log.debug("✓ Subdomain format valid: {}", request.getSubdomain());

        if (tenantRepository.findBySubdomain(request.getSubdomain()).isPresent()) {
            log.warn("✗ Subdomain already exists: {}", request.getSubdomain());
            throw new TenantAlreadyExistsException(request.getSubdomain());
        }
        log.debug("✓ Subdomain available: {}", request.getSubdomain());



        Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseThrow(() -> {
                    log.error("✗ Admin role not found: {}", ROLE_ADMIN);
                    return new RoleNotFoundException(ROLE_ADMIN);
                });
        log.debug("✓ Admin role found: {}", adminRole.getName());

        log.info("Phase 1: Pre-validation completed ✓");

        log.info("Phase 2: Creating tenant record");

        Tenant newTenant = authHelper.buildTenant(request);
        newTenant.setStatus(TENANT_STATUS_PROVISIONING); // Set to PROVISIONING
        Tenant savedTenant = tenantRepository.save(newTenant);

        log.info("✓ Tenant record created: id={}, subdomain={}", savedTenant.getId(), savedTenant.getSubdomain());


        log.info("Phase 3: Publishing TenantCreatedEvent");

        eventPublisher.publishEvent(new TenantCreatedEvent(savedTenant, request));

        log.info("✓ Event published. Synchronous registration complete.");
        log.info("========================================");

    }
}