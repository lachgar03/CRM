package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.events.TenantCreatedEvent;
import com.crm.AuthService.migration.FlywayMigrationService;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.security.TenantContextHolder;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous service to handle the provisioning of a new tenant schema
 * and admin user after the tenant record has been created.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final FlywayMigrationService flywayMigrationService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthHelper authHelper;

    private static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    private static final String TENANT_STATUS_PROVISIONING_FAILED = "PROVISIONING_FAILED";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Async
    @EventListener(TenantCreatedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Run in a new, separate transaction
    public void handleTenantCreation(TenantCreatedEvent event) {
        Tenant tenant = event.getTenant();
        TenantRegistrationRequest request = event.getRegistrationRequest();
        String schemaName = "tenant_" + tenant.getId();
        boolean schemaCreated = false;

        log.info("ASYNC: Starting provisioning for tenant: {}", tenant.getSubdomain());

        try {
            // 1. Provision Schema
            flywayMigrationService.createAndMigrateTenantSchema(tenant.getId());
            schemaCreated = true;
            log.info("ASYNC: ✓ Schema provisioned: {}", schemaName);

            // 2. Create Admin User (within tenant context)
            User adminUser;
            try {
                TenantContextHolder.setTenantId(tenant.getId());
                log.debug("ASYNC: Tenant context set for user creation: {}", tenant.getId());

                Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                        .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found during provisioning"));

                adminUser = authHelper.buildAdminUser(request, adminRole, tenant);
                userRepository.save(adminUser);

                log.info("ASYNC: ✓ Admin user created: {}", adminUser.getEmail());

            } finally {
                TenantContextHolder.clear();
                log.debug("ASYNC: Tenant context cleared");
            }

            // 3. Mark Tenant as Active
            tenant.setStatus(TENANT_STATUS_ACTIVE);
            tenant.setSchemaName(schemaName);
            tenantRepository.save(tenant);

            log.info("ASYNC: ✓ Tenant provisioning complete: {}", tenant.getSubdomain());

            // TODO: This is a great place to publish another event,
            // e.g., SendWelcomeEmailEvent(adminUser, tenant)

        } catch (Exception e) {
            log.error("ASYNC: ✗ Tenant provisioning failed for {}: {}", tenant.getSubdomain(), e.getMessage(), e);

            // Rollback: Set status to FAILED
            // We must find the tenant again as we are in a new transaction
            Tenant failedTenant = tenantRepository.findById(tenant.getId()).orElse(tenant);
            failedTenant.setStatus(TENANT_STATUS_PROVISIONING_FAILED);
            tenantRepository.save(failedTenant);

            // Rollback: Drop schema if it was created
            if (schemaCreated) {
                try {
                    flywayMigrationService.dropSchema(schemaName);
                    log.info("ASYNC: ✓ Schema rollback successful: {}", schemaName);
                } catch (Exception rollbackEx) {
                    log.error("ASYNC: ✗ Failed to rollback schema: {}", schemaName, rollbackEx);
                }
            }
        }
    }
}