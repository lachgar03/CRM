package com.crm.AuthService.config;

// IMPORTS for Redis
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
// REMOVED: Caffeine imports

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// REMOVED: Profile import
// REMOVED: TimeUnit import

import java.time.Duration; // IMPORT Duration

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures the Redis Cache Manager.
     * We set a default expiration time and configure caches to store
     * values as JSON for better readability in Redis.
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // Set a default expiration for all caches
                .entryTtl(Duration.ofMinutes(10))
                // Don't cache null values
                .disableCachingNullValues()
                // Serialize values as JSON
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                ));
    }

    /**
     * Customizer to apply our default configuration to all caches created
     * by the RedisCacheManager.
     * * This ensures that any new @Cacheable("new-cache-name") will
     * automatically get the 10-minute TTL and JSON serialization.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        // We can pre-configure specific caches if they need different TTLs
        return (builder) -> builder
                .withCacheConfiguration("userPermissions",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("permissionChecks",
                        cacheConfiguration().entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("roles",
                        cacheConfiguration().entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("allRoles",
                        cacheConfiguration().entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("tenants",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(60)));

        // Any other cache names used (like "users", "userDetails", etc.)
        // will automatically get the 10-minute default TTL.
    }
}