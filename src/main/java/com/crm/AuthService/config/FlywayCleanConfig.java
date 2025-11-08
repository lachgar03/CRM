package com.crm.AuthService.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

@Slf4j
@Configuration
@Profile("dev") // Only active in dev profile
@ConditionalOnProperty(name = "app.flyway.clean-on-startup", havingValue = "true")
@RequiredArgsConstructor
public class FlywayCleanConfig {

    private final DataSource dataSource;

    @Value("${app.flyway.clean-on-startup:false}")
    private boolean cleanOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1) // Run before other initialization
    public void cleanDatabase() {
        if (!cleanOnStartup) {
            return;
        }

        log.warn("⚠️  CLEANING DATABASE - All data will be lost!");
        log.warn("⚠️  This is only enabled in DEV profile");

        try {
            // Clean the shared schema
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas("public")
                    .cleanDisabled(false)
                    .load();

            flyway.clean();
            log.info("✓ Database cleaned successfully");

        } catch (Exception e) {
            log.error("✗ Failed to clean database", e);
            throw new RuntimeException("Database clean failed", e);
        }
    }
}