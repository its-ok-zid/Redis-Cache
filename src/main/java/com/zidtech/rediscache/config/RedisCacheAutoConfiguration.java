package com.zidtech.rediscache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zidtech.rediscache.api.RedisCacheClient;
import com.zidtech.rediscache.service.RedisCacheService;
import com.zidtech.rediscache.strategy.RedisCacheStrategies;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass({RedisTemplate.class, JedisConnectionFactory.class})
@EnableConfigurationProperties({RedisCacheProperties.class, RedisProperties.class})
public class RedisCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public JedisConnectionFactory jedisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                redisProperties.getHost(), redisProperties.getPort());

        if (redisProperties.getPassword() != null) {
            standalone.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        if (redisProperties.getUsername() != null && !redisProperties.getUsername().isBlank()) {
            standalone.setUsername(redisProperties.getUsername());
        }

        standalone.setDatabase(redisProperties.getDatabase());

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        if (redisProperties.getJedis() != null && redisProperties.getJedis().getPool() != null) {
            RedisProperties.Pool pool = redisProperties.getJedis().getPool();
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            if (pool.getMaxWait() != null) {
                poolConfig.setMaxWait(pool.getMaxWait());
            }
        }

        JedisClientConfiguration.JedisPoolingClientConfigurationBuilder clientConfigBuilder =
                JedisClientConfiguration.builder().usePooling();

        clientConfigBuilder.poolConfig(poolConfig);
        if (redisProperties.getConnectTimeout() != null) {
            clientConfigBuilder.connectTimeout(redisProperties.getConnectTimeout());
        }
        if (redisProperties.getTimeout() != null) {
            clientConfigBuilder.readTimeout(redisProperties.getTimeout());
        }

        return new JedisConnectionFactory(standalone, clientConfigBuilder.build());
    }

    @Bean(name = "redisCacheTemplate")
    @ConditionalOnMissingBean(name = "redisCacheTemplate")
    public RedisTemplate<String, Object> redisCacheTemplate(RedisConnectionFactory connectionFactory,
                                                             ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                          RedisCacheProperties properties,
                                          ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfiguration = cacheConfiguration(properties, objectMapper)
                .entryTtl(resolveTtl(properties.getDefaultTtl()));

        if (!properties.isCacheNullValues()) {
            defaultConfiguration = defaultConfiguration.disableCachingNullValues();
        }

        Map<String, RedisCacheConfiguration> perCacheConfiguration = new HashMap<>();
        properties.getCacheTtls().forEach((cacheName, ttl) ->
                perCacheConfiguration.put(
                        cacheName,
                        cacheConfiguration(properties, objectMapper).entryTtl(resolveTtl(ttl))
                )
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(perCacheConfiguration)
                .transactionAware()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheClient redisCacheClient(RedisTemplate<String, Object> redisCacheTemplate,
                                             RedisCacheProperties properties,
                                             ObjectMapper objectMapper) {
        return new RedisCacheService(redisCacheTemplate, properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheStrategies redisCacheStrategies(RedisCacheClient redisCacheClient) {
        return new RedisCacheStrategies(redisCacheClient);
    }

    private RedisCacheConfiguration cacheConfiguration(RedisCacheProperties properties, ObjectMapper objectMapper) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        if (!properties.getKeyPrefix().isBlank()) {
            configuration = configuration.computePrefixWith(cacheName -> properties.getKeyPrefix() + cacheName + "::");
        }

        return configuration;
    }

    private Duration resolveTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return Duration.ZERO;
        }
        return ttl;
    }
}
