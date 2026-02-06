package com.zidtech.rediscache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zidtech.rediscache.api.RedisCacheClient;
import com.zidtech.rediscache.config.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class RedisCacheService implements RedisCacheClient {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public <T> void put(String key, T value) {
        put(key, value, properties.getDefaultTtl());
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        String resolvedKey = withPrefix(key);
        redisTemplate.opsForValue().set(resolvedKey, value);
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.expire(resolvedKey, ttl);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> targetType) {
        Object value = redisTemplate.opsForValue().get(withPrefix(key));
        if (value == null) {
            return Optional.empty();
        }

        if (targetType.isInstance(value)) {
            return Optional.of(targetType.cast(value));
        }

        try {
            return Optional.of(objectMapper.convertValue(value, targetType));
        } catch (IllegalArgumentException ex) {
            log.warn("Unable to convert cached value for key={} into {}", key, targetType.getSimpleName(), ex);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String key) {
        Boolean deleted = redisTemplate.delete(withPrefix(key));
        return Boolean.TRUE.equals(deleted);
    }

    @Override
    public long deleteByPattern(String pattern) {
        Set<String> keys = keys(pattern);
        if (keys.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0 : deleted;
    }

    @Override
    public Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(withPrefix(pattern));
        return keys == null ? Collections.emptySet() : keys;
    }

    @Override
    public long count(String pattern) {
        return keys(pattern).size();
    }

    @Override
    public Optional<Duration> ttl(String key) {
        Long ttlSeconds = redisTemplate.getExpire(withPrefix(key), TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds < 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(ttlSeconds));
    }

    private String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return prefix == null || prefix.isBlank() ? key : prefix + key;
    }
}
