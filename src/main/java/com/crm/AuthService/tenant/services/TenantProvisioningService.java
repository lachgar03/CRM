package com.crm.AuthService.tenant.services;

import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.auth.services.AuthHelper;
import com.crm.AuthService.events.TenantCreatedEvent;
import com.crm.AuthService.exception.EmailAlreadyExistsException;
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
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String MASTER_TENANT_SUBDOMAIN = "admin";
    @Async
    @EventListener(TenantCreatedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTenantCreation(TenantCreatedEvent event) {
        Tenant tenant = event.getTenant();
        TenantRegistrationRequest request = event.getRegistrationRequest();
        String schemaName = "tenant_" + tenant.getId();
        boolean schemaCreated = false;

        log.info("ASYNC: Starting provisioning for tenant: {}", tenant.getSubdomain());

        try {
            flywayMigrationService.createAndMigrateTenantSchema(tenant.getId());
            schemaCreated = true;
            log.info("ASYNC: ✓ Schema provisioned: {}", schemaName);

            User adminUser;
            try {
                TenantContextHolder.setTenantId(tenant.getId());
                log.debug("ASYNC: Tenant context set for user creation: {}", tenant.getId());
                if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
                    log.warn("ASYNC: ✗ Email already exists in this tenant: {}", request.getAdminEmail());
                    throw new EmailAlreadyExistsException(request.getAdminEmail());
                }
                log.debug("ASYNC: ✓ Email available in new tenant: {}", request.getAdminEmail());

                String roleToAssign = tenant.getSubdomain().equalsIgnoreCase(MASTER_TENANT_SUBDOMAIN)
                                         ? ROLE_SUPER_ADMIN
                                           : ROLE_ADMIN;

                                       log.info("Assigning role '{}' to admin of tenant '{}'", roleToAssign, tenant.getSubdomain());

                                       Role adminRole = roleRepository.findByName(roleToAssign)
                                               .orElseThrow(() -> new RuntimeException(roleToAssign + " not found during provisioning"));
                adminUser = authHelper.buildAdminUser(request, adminRole, tenant);
                userRepository.save(adminUser);

                log.info("ASYNC: ✓ Admin user created: {}", adminUser.getEmail());

            } finally {
                TenantContextHolder.clear();
                log.debug("ASYNC: Tenant context cleared");
            }

            tenant.setStatus(TENANT_STATUS_ACTIVE);
            tenant.setSchemaName(schemaName);
            tenantRepository.save(tenant);

            log.info("ASYNC: ✓ Tenant provisioning complete: {}", tenant.getSubdomain());



        } catch (Exception e) {
            log.error("ASYNC: ✗ Tenant provisioning failed for {}: {}", tenant.getSubdomain(), e.getMessage(), e);


            Tenant failedTenant = tenantRepository.findById(tenant.getId()).orElse(tenant);
            failedTenant.setStatus(TENANT_STATUS_PROVISIONING_FAILED);
            tenantRepository.save(failedTenant);

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