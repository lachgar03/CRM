package com.crm.AuthService.config;

import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlywayMigrationService {

    private final DataSource dataSource;
    private final TenantRepository tenantRepository;

    /**
     * Migrate shared schema (public)
     * Runs on application startup
     */
    public void migrateSharedSchema() {
        log.info("=== Starting Shared Schema Migration ===");

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/shared")
                    .schemas("public")
                    .table("flyway_schema_history_shared")
                    .baselineOnMigrate(true)
                    .validateOnMigrate(true)
                    .load();

            MigrationInfo[] pending = flyway.info().pending();
            log.info("Pending migrations for shared schema: {}", pending.length);

            flyway.migrate();
            log.info("=== Shared Schema Migration Completed ===");

        } catch (Exception e) {
            log.error("Failed to migrate shared schema", e);
            throw new RuntimeException("Shared schema migration failed", e);
        }
    }

    /**
     * Migrate a specific tenant schema
     * @param schemaName Schema name (e.g., "tenant_1")
     */
    public void migrateTenantSchema(String schemaName) {
        log.info("Migrating tenant schema: {}", schemaName);

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/tenant")
                    .schemas(schemaName)
                    .table("flyway_schema_history")
                    .baselineOnMigrate(true)
                    .validateOnMigrate(true)
                    .load();

            int migrationsApplied = flyway.migrate().migrationsExecuted;
            log.info("Applied {} migrations to schema: {}", migrationsApplied, schemaName);

        } catch (Exception e) {
            log.error("Failed to migrate tenant schema: {}", schemaName, e);
            throw new RuntimeException("Tenant schema migration failed: " + schemaName, e);
        }
    }

    /**
     * Create and initialize a new tenant schema
     * This is called automatically during tenant registration
     *
     * @param tenantId Tenant ID
     * @return Schema name created
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createAndMigrateTenantSchema(Long tenantId) {
        String schemaName = "tenant_" + tenantId;

        log.info("=== Creating New Tenant Schema: {} ===", schemaName);

        try {
            // Step 1: Create the schema
            createSchema(schemaName);

            // Step 2: Run all migrations on the new schema
            migrateTenantSchema(schemaName);

            // Step 3: Verify schema was created successfully
            verifySchema(schemaName);

            log.info("=== Tenant Schema Ready: {} ===", schemaName);
            return schemaName;

        } catch (Exception e) {
            log.error("Failed to create/migrate tenant schema: {}", schemaName, e);

            // Attempt cleanup on failure
            try {
                dropSchema(schemaName);
                log.info("Rolled back schema creation: {}", schemaName);
            } catch (Exception cleanupEx) {
                log.error("Failed to cleanup schema after error: {}", schemaName, cleanupEx);
            }

            throw new RuntimeException("Tenant schema initialization failed", e);
        }
    }

    /**
     * Migrate all existing tenant schemas
     * Called on application startup to update all tenants
     */
    public void migrateAllTenantSchemas() {
        log.info("=== Migrating All Existing Tenant Schemas ===");

        List<Tenant> tenants = tenantRepository.findAll();
        log.info("Found {} tenant(s) to migrate", tenants.size());

        int successCount = 0;
        int failureCount = 0;

        for (Tenant tenant : tenants) {
            String schemaName = "tenant_" + tenant.getId();
            try {
                migrateTenantSchema(schemaName);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to migrate schema for tenant {}: {}",
                        tenant.getSubdomain(), e.getMessage());
                failureCount++;
                // Continue with other tenants
            }
        }

        log.info("=== Tenant Schema Migration Summary: {} succeeded, {} failed ===",
                successCount, failureCount);
    }

    /**
     * Create a new PostgreSQL schema
     */
    private void createSchema(String schemaName) {
        log.info("Creating schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create schema
            statement.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName));

            // Set search path (optional, for better query performance)
            statement.execute(String.format("ALTER DATABASE %s SET search_path TO %s, public",
                    connection.getCatalog(), schemaName));

            log.info("Schema created successfully: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to create schema: {}", schemaName, e);
            throw new RuntimeException("Schema creation failed", e);
        }
    }

    /**
     * Drop a schema (used for cleanup on failure)
     */
    public void dropSchema(String schemaName) {
        log.warn("Dropping schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName));
            log.info("Schema dropped: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to drop schema: {}", schemaName, e);
            throw new RuntimeException("Schema cleanup failed", e);
        }
    }

    /**
     * Verify schema exists and has expected tables
     */
    private void verifySchema(String schemaName) {
        log.info("Verifying schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String query = String.format(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = '%s'", schemaName);

            var resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                int tableCount = resultSet.getInt(1);
                log.info("Schema {} has {} table(s)", schemaName, tableCount);

                if (tableCount == 0) {
                    throw new RuntimeException("Schema created but has no tables");
                }
            }

        } catch (Exception e) {
            log.error("Schema verification failed: {}", schemaName, e);
            throw new RuntimeException("Schema verification failed", e);
        }
    }

    /**
     * Get migration status for a tenant schema
     */
    public String getMigrationStatus(Long tenantId) {
        String schemaName = "tenant_" + tenantId;

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/tenant")
                    .schemas(schemaName)
                    .table("flyway_schema_history")
                    .load();

            var info = flyway.info();
            return String.format("Schema: %s, Applied: %d, Pending: %d",
                    schemaName,
                    info.applied().length,
                    info.pending().length);

        } catch (Exception e) {
            return "Error checking status: " + e.getMessage();
        }
    }
}