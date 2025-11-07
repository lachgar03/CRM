package com.crm.AuthService.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {


    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                ));
    }


    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
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


    }
}