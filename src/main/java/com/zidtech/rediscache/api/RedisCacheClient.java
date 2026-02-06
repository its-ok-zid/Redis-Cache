package com.zidtech.rediscache.api;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface RedisCacheClient {

    <T> void put(String key, T value);

    <T> void put(String key, T value, Duration ttl);

    <T> Optional<T> get(String key, Class<T> targetType);

    boolean delete(String key);

    long deleteByPattern(String pattern);

    Set<String> keys(String pattern);

    long count(String pattern);

    Optional<Duration> ttl(String key);
}
