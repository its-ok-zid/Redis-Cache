package com.zidtech.rediscache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "zidtech.redis.cache")
public class RedisCacheProperties {

    /** Prefix added to all cache keys managed by this library. */
    private String keyPrefix = "";

    /** Default TTL for programmatic put() operations and cache manager defaults. */
    private Duration defaultTtl = Duration.ofMinutes(10);

    /** Optional cache-specific TTLs for Spring CacheManager caches. */
    private Map<String, Duration> cacheTtls = new HashMap<>();

    /** Whether null values should be cached via CacheManager. */
    private boolean cacheNullValues = false;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getCacheTtls() {
        return cacheTtls;
    }

    public void setCacheTtls(Map<String, Duration> cacheTtls) {
        this.cacheTtls = cacheTtls == null ? new HashMap<>() : cacheTtls;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }
}
