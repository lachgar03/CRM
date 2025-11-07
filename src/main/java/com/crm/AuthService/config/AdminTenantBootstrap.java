package com.crm.AuthService.config;

import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.auth.services.TenantRegistrationService;
import com.crm.AuthService.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminTenantBootstrap implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final TenantRegistrationService tenantRegistrationService;

    // Injecter les valeurs de application.yml
    @Value("${app.super-admin.email}")
    private String adminEmail;
    @Value("${app.super-admin.password}")
    private String adminPassword;
    @Value("${app.super-admin.first-name}")
    private String adminFirstName;
    @Value("${app.super-admin.last-name}")
    private String adminLastName;

    private final String MASTER_TENANT_SUBDOMAIN = "admin";

    @Override
    public void run(String... args) throws Exception {
        // 1. Vérifier si le tenant "admin" existe déjà
        if (tenantRepository.findBySubdomain(MASTER_TENANT_SUBDOMAIN).isEmpty()) {

            log.warn("=== 🚀 MASTER TENANT '{}' NOT FOUND. BOOTSTRAPPING... ===", MASTER_TENANT_SUBDOMAIN);

            // 2. S'il n'existe pas, créer la demande d'enregistrement
            TenantRegistrationRequest request = new TenantRegistrationRequest();
            request.setCompanyName("CRM Master Admin");
            request.setSubdomain(MASTER_TENANT_SUBDOMAIN);
            request.setAdminEmail(adminEmail);
            request.setAdminPassword(adminPassword);
            request.setAdminFirstName(adminFirstName);
            request.setAdminLastName(adminLastName);
            request.setSubscriptionPlan("ENTERPRISE"); // Donnez-lui un plan spécial

            try {
                // Cette méthode va créer le Tenant ET publier l'événement
                // que TenantProvisioningService (Étape 1) va intercepter.
                tenantRegistrationService.registerTenant(request);
                log.info("=== ✅ Master tenant '{}' registration request submitted. ===", MASTER_TENANT_SUBDOMAIN);
                log.info("=== Provisioning will complete asynchronously. ===");
            } catch (Exception e) {
                log.error("=== ✗ FAILED TO SUBMIT MASTER TENANT REGISTRATION ===", e);
            }
        } else {
            log.info("=== Master tenant '{}' already exists. Skipping bootstrap. ===", MASTER_TENANT_SUBDOMAIN);
        }
    }
}