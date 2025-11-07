package com.crm.AuthService.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaResolver {

    private final DataSource dataSource;


    public void setCurrentTenantSchema(Long tenantId) {
        String schemaName = "tenant_" + tenantId;
        log.debug("Setting schema search_path to: {}, public", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(String.format("SET search_path TO %s, public", schemaName));

        } catch (SQLException e) {
            log.error("Failed to set search_path for schema: {}", schemaName, e);
            throw new RuntimeException("Could not set tenant schema", e);
        }
    }


    public void clearCurrentTenantSchema() {
        log.debug("Resetting schema search_path to default (public)");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SET search_path TO public");

        } catch (SQLException e) {
            log.error("Failed to reset search_path", e);
        }
    }
}