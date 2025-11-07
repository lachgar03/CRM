package com.crm.AuthService.security;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_SUBDOMAIN = new ThreadLocal<>();


    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("Attempting to set null tenantId in TenantContext");
            return;
        }
        TENANT_ID.set(tenantId);
        log.debug("Tenant context set: tenantId={}", tenantId);
    }


    public static Long getTenantId() {
        return TENANT_ID.get();
    }


    public static Long getRequiredTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set for this request");
        }
        return tenantId;
    }


    public static void setTenantSubdomain(String subdomain) {
        TENANT_SUBDOMAIN.set(subdomain);
    }


    public static String getTenantSubdomain() {
        return TENANT_SUBDOMAIN.get();
    }


    public static boolean isSet() {
        return TENANT_ID.get() != null;
    }


    public static void clear() {
        Long tenantId = TENANT_ID.get();
        if (tenantId != null) {
            log.debug("Clearing tenant context: tenantId={}", tenantId);
        }
        TENANT_ID.remove();
        TENANT_SUBDOMAIN.remove();
    }


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