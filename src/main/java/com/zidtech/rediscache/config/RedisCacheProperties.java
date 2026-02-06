package com.zidtech.rediscache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "zidtech.redis.cache")
public class RedisCacheProperties {

    /**
     * Prefix added to all keys written/read by this library.
     */
    private String keyPrefix = "";

    /**
     * Default TTL for cache keys. If null/zero/negative then no expiry is set.
     */
    private Duration defaultTtl = Duration.ofMinutes(10);

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
}
