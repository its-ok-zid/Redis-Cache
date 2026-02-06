# Redis Cache Library

A reusable Redis cache library for Spring Boot microservices.

## What this library provides

- A simple `RedisCacheClient` API to read/write/evict/cache by pattern.
- Auto-configuration for Redis serialization (`String` keys + JSON values).
- Externalized properties for key prefix and default TTL.

## Coordinates

```xml
<dependency>
  <groupId>com.zidtech</groupId>
  <artifactId>redis-cache-library</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration in consumer microservice

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

zidtech:
  redis:
    cache:
      key-prefix: "e-cart:"
      default-ttl: 15m
```

## Usage

```java
import com.zidtech.rediscache.api.RedisCacheClient;

@Service
@RequiredArgsConstructor
public class ProductFacade {
    private final RedisCacheClient redisCacheClient;

    public ProductDto findProduct(String id) {
        return redisCacheClient.get("product:" + id, ProductDto.class)
                .orElseGet(() -> {
                    ProductDto dto = loadFromDatabase(id);
                    redisCacheClient.put("product:" + id, dto);
                    return dto;
                });
    }
}
```

## Publish to GitHub Packages

1. Update `distributionManagement.repository.url` in `pom.xml` to your repo URL.
2. Add credentials in Maven `settings.xml`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>GITHUB_USERNAME</username>
    <password>GITHUB_TOKEN</password>
  </server>
</servers>
```

3. Publish:

```bash
mvn clean deploy
```
