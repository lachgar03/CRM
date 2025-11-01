package com.crm.AuthService.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!prod")
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "userPermissions",
                "users",
                "roles",
                "allRoles",
                "tenants",
                "permissionChecks"
        );

        cacheManager.setCaffeine(CacheProperties.Caffeine.newBuilder()
                .maximumSize(1000)  // Max 1000 entries per cache
                .expireAfterWrite(10, TimeUnit.MINUTES)  // Expire after 10 minutes
                .recordStats());  // Enable cache statistics

        return cacheManager;
    }
}
