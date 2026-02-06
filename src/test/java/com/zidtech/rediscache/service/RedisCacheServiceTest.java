package com.zidtech.rediscache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zidtech.rediscache.config.RedisCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheServiceTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisCacheProperties properties = new RedisCacheProperties();
        properties.setKeyPrefix("svc:");
        properties.setDefaultTtl(Duration.ofMinutes(5));

        redisCacheService = new RedisCacheService(redisTemplate, properties, new ObjectMapper());
    }

    @Test
    void shouldUseDefaultTtlWhenPuttingValue() {
        redisCacheService.put("product:1", Map.of("name", "Phone"));

        verify(valueOperations).set(eq("svc:product:1"), any());
        verify(redisTemplate).expire("svc:product:1", Duration.ofMinutes(5));
    }

    @Test
    void shouldSkipExpiryWhenTtlIsZero() {
        redisCacheService.put("product:2", "value", Duration.ZERO);

        verify(valueOperations).set("svc:product:2", "value");
        verify(redisTemplate, never()).expire(eq("svc:product:2"), any(Duration.class));
    }

    @Test
    void shouldConvertCachedMapToTargetType() {
        when(valueOperations.get("svc:user:1")).thenReturn(Map.of("name", "Asha"));

        Optional<UserView> user = redisCacheService.get("user:1", UserView.class);

        assertThat(user).isPresent();
        assertThat(user.get().name()).isEqualTo("Asha");
    }

    @Test
    void shouldDeleteByPattern() {
        when(redisTemplate.keys("svc:product:*"))
                .thenReturn(Set.of("svc:product:1", "svc:product:2"));
        when(redisTemplate.delete(Set.of("svc:product:1", "svc:product:2"))).thenReturn(2L);

        long deleted = redisCacheService.deleteByPattern("product:*");

        assertThat(deleted).isEqualTo(2);
    }

    private record UserView(String name) {
    }
}
