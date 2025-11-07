package com.crm.AuthService.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final CacheManager cacheManager;


    public void evictUserCaches(Long userId, String email) {
        evictCache("users", email);
        evictCache("userPermissions", userId);
        log.debug("Evicted user caches: userId={}, email={}", userId, email);
    }


    public void evictAllUserPermissions() {
        clearCache("userPermissions");
        clearCache("permissionChecks");
        log.info("Evicted all user permission caches");
    }


    public void evictRoleCaches(Long roleId) {
        evictCache("roles", roleId);
        clearCache("allRoles");
        evictAllUserPermissions(); // Roles affect user permissions
        log.debug("Evicted role caches: roleId={}", roleId);
    }


    public void evictTenantCache(Long tenantId) {
        evictCache("tenants", tenantId);
        log.debug("Evicted tenant cache: tenantId={}", tenantId);
    }


    public void evictTenantCacheBySubdomain(String subdomain) {
        evictCache("tenants", subdomain);
        log.debug("Evicted tenant cache: subdomain={}", subdomain);
    }


    private void evictCache(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }


    private void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

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