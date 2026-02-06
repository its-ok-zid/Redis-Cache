# Redis Cache Library (Jedis-based)

Reusable Redis cache library for Spring Boot microservices, designed for shared usage across services such as `e-cart`, `product`, etc.

## What is included

- Jedis client + `JedisConnectionFactory` auto-configuration.
- `RedisTemplate<String,Object>` with JSON value serialization.
- `RedisCacheManager` with:
  - default TTL,
  - per-cache TTL overrides,
  - optional null caching,
  - transaction-aware cache behavior.
- Programmatic cache API via `RedisCacheClient`.
- Strategy helpers for common patterns:
  - Cache-Aside
  - Read-Through
  - Write-Through
  - Write-Behind
  - Invalidation

## Maven dependency

```xml
<dependency>
  <groupId>com.zidtech</groupId>
  <artifactId>redis-cache-library</artifactId>
  <version>1.1.0-SNAPSHOT</version>
</dependency>
```

## Consumer service configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 2s
      connect-timeout: 2s
      jedis:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 2
          max-wait: 2s

zidtech:
  redis:
    cache:
      key-prefix: "e-cart:"
      default-ttl: 15m
      cache-null-values: false
      cache-ttls:
        productById: 30m
        categoryList: 60m
        searchResults: 5m
```

## Programmatic usage

```java
@Service
@RequiredArgsConstructor
public class ProductCacheFacade {
    private final RedisCacheClient redisCacheClient;

    public ProductDto findById(String id) {
        return redisCacheClient.get("product:" + id, ProductDto.class)
                .orElseGet(() -> {
                    ProductDto dto = loadFromDb(id);
                    redisCacheClient.put("product:" + id, dto);
                    return dto;
                });
    }
}
```

## Strategy helper usage

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final RedisCacheStrategies cacheStrategies;

    public ProductDto getProduct(String id) {
        return cacheStrategies.cacheAside(
                "product:" + id,
                ProductDto.class,
                () -> loadFromDb(id),
                Duration.ofMinutes(20)
        );
    }
}
```

## Publish to GitHub Packages

1. Update `distributionManagement.repository.url` in `pom.xml`.
2. Add GitHub credentials in `~/.m2/settings.xml` for server id `github`.
3. Publish:

```bash
mvn clean deploy
```
