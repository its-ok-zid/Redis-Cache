package com.zidtech.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Cache any value with optional TTL (in seconds). If ttlSeconds <= 0, do not set expiry. */
    public void cacheValue(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value);
            if (ttlSeconds > 0) {
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Error caching key: {} error: {}", key, e.getMessage());
        }
    }

    public void cacheValue(String key, Object value) {
        cacheValue(key, value, 0);
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error getting key: {} error: {}", key, e.getMessage());
            return null;
        }
    }

    public void evictKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Error deleting key: {} error: {}", key, e.getMessage());
        }
    }

    /** returns count of keys matching pattern - note: using KEYS on production is discouraged */
    public long getCacheSize(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys == null ? 0 : keys.size();
        } catch (Exception e) {
            log.error("Error getting keys for pattern: {} error: {}", pattern, e.getMessage());
            return 0;
        }
    }

    /** Get TTL for a key **/
    public Long getTtl(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            return -1L;
        }
    }
}