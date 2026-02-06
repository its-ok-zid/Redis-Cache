package com.zidtech.rediscache.strategy;

import com.zidtech.rediscache.api.RedisCacheClient;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable strategy helpers for common caching patterns used in microservices.
 */
public class RedisCacheStrategies {

    private final RedisCacheClient redisCacheClient;

    public RedisCacheStrategies(RedisCacheClient redisCacheClient) {
        this.redisCacheClient = redisCacheClient;
    }

    /**
     * Cache-aside / lazy-loading strategy.
     */
    public <T> T cacheAside(String key, Class<T> targetType, Supplier<T> dbLoader, Duration ttl) {
        Optional<T> cached = redisCacheClient.get(key, targetType);
        if (cached.isPresent()) {
            return cached.get();
        }
        T loaded = dbLoader.get();
        if (loaded != null) {
            redisCacheClient.put(key, loaded, ttl);
        }
        return loaded;
    }

    /**
     * Read-through style strategy: read from cache first, fallback to source provider.
     */
    public <T> T readThrough(String key, Class<T> targetType, Supplier<T> sourceProvider) {
        return cacheAside(key, targetType, sourceProvider, null);
    }

    /**
     * Write-through strategy: write to source first and then update cache.
     */
    public <T> T writeThrough(String key, T value, Consumer<T> sourceWriter, Duration ttl) {
        sourceWriter.accept(value);
        redisCacheClient.put(key, value, ttl);
        return value;
    }

    /**
     * Write-behind strategy: enqueue/async source persistence externally, but cache immediately.
     */
    public <T> void writeBehind(String key, T value, Duration ttl, Consumer<T> asyncSourceWriter) {
        redisCacheClient.put(key, value, ttl);
        asyncSourceWriter.accept(value);
    }

    /**
     * Explicit invalidation strategy for cache coherence after updates/deletes.
     */
    public void invalidate(String key) {
        redisCacheClient.delete(key);
    }
}
