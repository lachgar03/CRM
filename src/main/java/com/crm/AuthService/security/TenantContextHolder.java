package com.crm.AuthService.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local holder for tenant context information
 * Stores the current tenant ID for the duration of the HTTP request
 */
@Slf4j
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_SUBDOMAIN = new ThreadLocal<>();

    /**
     * Set the current tenant ID
     * @param tenantId The tenant identifier
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("Attempting to set null tenantId in TenantContext");
            return;
        }
        TENANT_ID.set(tenantId);
        log.debug("Tenant context set: tenantId={}", tenantId);
    }

    /**
     * Get the current tenant ID
     * @return The tenant identifier, or null if not set
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * Get the current tenant ID, throwing exception if not present
     * @return The tenant identifier
     * @throws IllegalStateException if tenant context is not set
     */
    public static Long getRequiredTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set for this request");
        }
        return tenantId;
    }

    /**
     * Set the current tenant subdomain (optional)
     * @param subdomain The tenant subdomain
     */
    public static void setTenantSubdomain(String subdomain) {
        TENANT_SUBDOMAIN.set(subdomain);
    }

    /**
     * Get the current tenant subdomain
     * @return The tenant subdomain, or null if not set
     */
    public static String getTenantSubdomain() {
        return TENANT_SUBDOMAIN.get();
    }

    /**
     * Check if tenant context is set
     * @return true if tenant ID is set, false otherwise
     */
    public static boolean isSet() {
        return TENANT_ID.get() != null;
    }

    /**
     * Clear the tenant context
     * MUST be called at the end of each request to prevent memory leaks
     */
    public static void clear() {
        Long tenantId = TENANT_ID.get();
        if (tenantId != null) {
            log.debug("Clearing tenant context: tenantId={}", tenantId);
        }
        TENANT_ID.remove();
        TENANT_SUBDOMAIN.remove();
    }

    /**
     * Execute a runnable with a specific tenant context
     * Useful for async operations or background jobs
     *
     * @param tenantId The tenant identifier
     * @param runnable The code to execute
     */
    public static void executeInTenantContext(Long tenantId, Runnable runnable) {
        Long previousTenantId = getTenantId();
        try {
            setTenantId(tenantId);
            runnable.run();
        } finally {
            if (previousTenantId != null) {
                setTenantId(previousTenantId);
            } else {
                clear();
            }
        }
    }
}