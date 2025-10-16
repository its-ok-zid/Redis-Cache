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

    /**
     * Manual Cache methods
     **/
    public void saveCache(String key, Object value) {
        try {
            redisTemplate.opsForValue()
                    .set(key, value, 1, TimeUnit.HOURS);
            log.info("Cache value for key:{}", key);
        } catch (Exception e) {
            log.error("Error cache for key:{}. Error:{}", key, e.getMessage());
        }
    }

    public void cacheValue(String key, Object value) {
        try {
            redisTemplate.opsForValue()
                    .set(key, value);
            log.info("Cache value for key:{}", key);
        } catch (Exception e) {
            log.error("Error cache for key:{}, with Error:{}", key, e.getMessage());
        }
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error getting cache for key:{}, with Error:{}", key, e.getMessage());
            return null;
        }
    }

    public <T> T getValue(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (type.isInstance(value)) {
                return type.cast(value);
            } else {
                log.warn("Cached value for key:{} is not of type:{}", key, type.getName());
                return null;
            }
        } catch (Exception e) {
            log.error("Error while getting cache for key:{}, with Error:{}", key, e.getMessage());
            return null;
        }
    }

   /** Pattern-based cache eviction **/
    public void evictPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error evicting pattern: {}", pattern, e);
        }
    }

    public void evictKey(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Key {} evicted: {}", key, deleted);
        } catch (Exception e) {
            log.error("Error evicting key: {}", key, e);
        }
    }

    /** Cache statistics and monitoring **/
    public Long getCacheSize(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? (long) keys.size() : 0L;
        } catch (Exception e) {
            log.error("Error getting cache size for pattern: {}", pattern, e);
            return 0L;
        }
    }

    public void clearAllCaches() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared all {} cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing all caches", e);
        }
    }

    /** Check if key exists **/
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error checking key existence: {}", key, e);
            return false;
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

