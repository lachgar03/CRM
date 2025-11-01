package com.crm.AuthService.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service to manage cache eviction across the application
 * Centralizes cache invalidation logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * Evict user-related caches when user is updated
     */
    public void evictUserCaches(Long userId, String email) {
        evictCache("users", email);
        evictCache("userPermissions", userId);
        log.debug("Evicted user caches: userId={}, email={}", userId, email);
    }

    /**
     * Evict all user permission caches (when roles/permissions change globally)
     */
    public void evictAllUserPermissions() {
        clearCache("userPermissions");
        clearCache("permissionChecks");
        log.info("Evicted all user permission caches");
    }

    /**
     * Evict role caches when role is updated
     */
    public void evictRoleCaches(Long roleId) {
        evictCache("roles", roleId);
        clearCache("allRoles");
        evictAllUserPermissions(); // Roles affect user permissions
        log.debug("Evicted role caches: roleId={}", roleId);
    }

    /**
     * Evict tenant cache when tenant is updated
     */
    public void evictTenantCache(Long tenantId) {
        evictCache("tenants", tenantId);
        log.debug("Evicted tenant cache: tenantId={}", tenantId);
    }

    /**
     * Evict tenant cache by subdomain
     */
    public void evictTenantCacheBySubdomain(String subdomain) {
        evictCache("tenants", subdomain);
        log.debug("Evicted tenant cache: subdomain={}", subdomain);
    }

    /**
     * Evict specific cache entry
     */
    private void evictCache(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    /**
     * Clear entire cache
     */
    private void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Clear all caches (use sparingly, e.g., after system configuration change)
     */
    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        log.warn("All caches cleared");
    }
}