package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.migration.FlywayMigrationService;
import com.crm.AuthService.exception.EmailAlreadyExistsException;
import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.exception.TenantAlreadyExistsException;
import com.crm.AuthService.exception.TenantProvisioningException;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.security.TenantContextHolder;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crm.AuthService.role.entities.Role;

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
    private static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String TENANT_STATUS_PROVISIONING = "PROVISIONING";
    private static final String TENANT_STATUS_PROVISIONING_FAILED = "PROVISIONING_FAILED";
    private final FlywayMigrationService flywayMigrationService;


    @Override
    @Transactional
    public AuthResponse registerTenant(TenantRegistrationRequest request) {
        log.info("========================================");
        log.info("Starting Tenant Registration");
        log.info("Subdomain: {}", request.getSubdomain());
        log.info("Admin Email: {}", request.getAdminEmail());
        log.info("========================================");

        Tenant savedTenant = null;
        String schemaName = null;
        boolean schemaCreated = false;

        try {
            // ============================================================
            // PHASE 1: PRE-VALIDATION
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
            // PHASE 2: TENANT RECORD CREATION
            // ============================================================
            log.info("Phase 2: Creating tenant record");

            Tenant newTenant = authHelper.buildTenant(request);
            newTenant.setStatus(TENANT_STATUS_PROVISIONING);
            savedTenant = tenantRepository.save(newTenant);

            log.info("✓ Tenant record created: id={}, subdomain={}", savedTenant.getId(), savedTenant.getSubdomain());

            // ============================================================
            // PHASE 3: SCHEMA PROVISIONING
            // ============================================================
            log.info("Phase 3: Provisioning tenant schema");

            schemaName = "tenant_" + savedTenant.getId();
            try {
                flywayMigrationService.createAndMigrateTenantSchema(savedTenant.getId());
                schemaCreated = true;

                log.info("✓ Schema provisioned successfully: {}", schemaName);

                savedTenant.setStatus(TENANT_STATUS_ACTIVE);
                savedTenant.setSchemaName(schemaName);
                savedTenant = tenantRepository.save(savedTenant);

                log.info("✓ Tenant status updated to ACTIVE");

            } catch (Exception e) {
                log.error("✗ Schema provisioning failed: {}", schemaName, e);

                savedTenant.setStatus(TENANT_STATUS_PROVISIONING_FAILED);
                tenantRepository.save(savedTenant);

                throw new TenantProvisioningException(
                        "Failed to provision tenant schema: " + e.getMessage(), e
                );
            }

            // ============================================================
            // PHASE 4: ADMIN USER CREATION
            // ============================================================
            log.info("Phase 4: Creating admin user");

            User adminUser;
            try {
                TenantContextHolder.setTenantId(savedTenant.getId());
                log.debug("Tenant context set: tenantId={}", savedTenant.getId());

                adminUser = authHelper.buildAdminUser(request, adminRole, savedTenant);
                adminUser = userRepository.save(adminUser);

                log.info("✓ Admin user created: id={}, email={}", adminUser.getId(), adminUser.getEmail());

            } catch (Exception e) {
                log.error("✗ Failed to create admin user", e);
                throw new RuntimeException("Failed to create admin user: " + e.getMessage(), e);
            } finally {
                TenantContextHolder.clear();
                log.debug("Tenant context cleared");
            }

            // ============================================================
            // PHASE 5: TOKEN GENERATION
            // ============================================================
            log.info("Phase 5: Generating authentication tokens");

            AuthResponse response = authHelper.buildAuthResponse(adminUser);

            log.info("✓ Tokens generated successfully");

            log.info("========================================");
            log.info("Tenant Registration Completed Successfully");
            log.info("Tenant ID: {}", savedTenant.getId());
            log.info("Subdomain: {}", savedTenant.getSubdomain());
            log.info("Schema: {}", schemaName);
            log.info("Admin: {}", adminUser.getEmail());
            log.info("Status: {}", savedTenant.getStatus());
            log.info("========================================");

            return response;

        } catch (TenantAlreadyExistsException | EmailAlreadyExistsException |
                 RoleNotFoundException | TenantProvisioningException e) {

            log.error("Registration failed: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during tenant registration", e);

            // Rollback schema
            if (schemaCreated && schemaName != null) {
                try {
                    flywayMigrationService.dropSchema(schemaName);
                    log.info("✓ Schema rollback successful: {}", schemaName);
                } catch (Exception rollbackEx) {
                    log.error("✗ Failed to rollback schema: {}", schemaName, rollbackEx);
                }
            }

            if (savedTenant != null) {
                try {
                    tenantRepository.delete(savedTenant);
                    log.info("✓ Tenant record rollback successful");
                } catch (Exception rollbackEx) {
                    log.error("✗ Failed to rollback tenant record", rollbackEx);
                }
            }

            throw new RuntimeException("Tenant registration failed: " + e.getMessage(), e);
        }
    }
}