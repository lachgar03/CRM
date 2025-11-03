package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
// REMOVED: AuthResponse
import com.crm.AuthService.events.TenantCreatedEvent; // IMPORT EVENT
// REMOVED: FlywayMigrationService
import com.crm.AuthService.exception.EmailAlreadyExistsException;
import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.exception.TenantAlreadyExistsException;
// REMOVED: TenantProvisioningException
import com.crm.AuthService.role.repositories.RoleRepository;
// REMOVED: TenantContextHolder
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
// REMOVED: User
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crm.AuthService.role.entities.Role;

import org.springframework.context.ApplicationEventPublisher; // IMPORT PUBLISHER
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

    // REMOVED: TENANT_STATUS_ACTIVE
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String TENANT_STATUS_PROVISIONING = "PROVISIONING";
    // REMOVED: TENANT_STATUS_PROVISIONING_FAILED
    // REMOVED: FlywayMigrationService


    @Override
    @Transactional // This transaction is now very short and fast
    public void registerTenant(TenantRegistrationRequest request) { // RETURN TYPE CHANGED
        log.info("========================================");
        log.info("Phase 1: Starting Tenant Registration (Synchronous)");
        log.info("Subdomain: {}", request.getSubdomain());
        log.info("Admin Email: {}", request.getAdminEmail());
        log.info("========================================");

        // ============================================================
        // PHASE 1: PRE-VALIDATION (Synchronous)
        // ============================================================
        log.info("Phase 1: Pre-validation checks");

        // 1.1 Validate subdomain
        authHelper.validateSubdomain(request.getSubdomain());
        log.debug("✓ Subdomain format valid: {}", request.getSubdomain());

        // 1.2 Check subdomain uniqueness
        if (tenantRepository.findBySubdomain(request.getSubdomain()).isPresent()) {
            log.warn("✗ Subdomain already exists: {}", request.getSubdomain());
            throw new TenantAlreadyExistsException(request.getSubdomain());
        }
        log.debug("✓ Subdomain available: {}", request.getSubdomain());

        // 1.3 Check email uniqueness
        // NOTE: This check is problematic as userRepository is tenant-scoped.
        // For a true global check, a separate public table is needed.
        // We leave it as-is for this refactor.
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            log.warn("✗ Email already exists: {}", request.getAdminEmail());
            throw new EmailAlreadyExistsException(request.getAdminEmail());
        }
        log.debug("✓ Email available: {}", request.getAdminEmail());

        // 1.4 Verify admin role exists
        Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseThrow(() -> {
                    log.error("✗ Admin role not found: {}", ROLE_ADMIN);
                    return new RoleNotFoundException(ROLE_ADMIN);
                });
        log.debug("✓ Admin role found: {}", adminRole.getName());

        log.info("Phase 1: Pre-validation completed ✓");

        // ============================================================
        // PHASE 2: TENANT RECORD CREATION (Synchronous)
        // ============================================================
        log.info("Phase 2: Creating tenant record");

        Tenant newTenant = authHelper.buildTenant(request);
        newTenant.setStatus(TENANT_STATUS_PROVISIONING); // Set to PROVISIONING
        Tenant savedTenant = tenantRepository.save(newTenant);

        log.info("✓ Tenant record created: id={}, subdomain={}", savedTenant.getId(), savedTenant.getSubdomain());

        // ============================================================
        // PHASE 3: PUBLISH EVENT for Asynchronous Provisioning
        // ============================================================
        log.info("Phase 3: Publishing TenantCreatedEvent");

        eventPublisher.publishEvent(new TenantCreatedEvent(savedTenant, request));

        log.info("✓ Event published. Synchronous registration complete.");
        log.info("========================================");

        // No AuthResponse is returned.
        // The extensive try/catch/rollback logic is GONE.
    }
}