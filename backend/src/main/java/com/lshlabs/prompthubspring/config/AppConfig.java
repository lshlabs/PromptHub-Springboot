package com.lshlabs.prompthubspring.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableCaching
public class AppConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") List<String> allowedMethods,
            @Value("${app.cors.allowed-headers:Authorization,Content-Type,X-Requested-With,X-Session-Key}") List<String> allowedHeaders,
            @Value("${app.cors.exposed-headers:Authorization}") List<String> exposedHeaders,
            @Value("${app.cors.allow-credentials:false}") boolean allowCredentials,
            @Value("${app.cors.max-age-seconds:3600}") long maxAgeSeconds) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(normalizeCsvList(allowedOrigins));
        config.setAllowedMethods(normalizeCsvList(allowedMethods));
        config.setAllowedHeaders(normalizeCsvList(allowedHeaders));
        config.setExposedHeaders(normalizeCsvList(exposedHeaders));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> normalizeCsvList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache statsCache = new CaffeineCache("stats",
                Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofSeconds(60)).build());

        CaffeineCache trendingCache = new CaffeineCache("trending", Caffeine.newBuilder().maximumSize(10_000).build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(statsCache, trendingCache));
        return manager;
    }
}
