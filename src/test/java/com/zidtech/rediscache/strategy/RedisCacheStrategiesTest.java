package com.zidtech.rediscache.strategy;

import com.zidtech.rediscache.api.RedisCacheClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisCacheStrategiesTest {

    @Test
    void cacheAsideLoadsFromSourceAndCachesWhenMiss() {
        RedisCacheClient client = mock(RedisCacheClient.class);
        when(client.get("k1", String.class)).thenReturn(Optional.empty());

        RedisCacheStrategies strategies = new RedisCacheStrategies(client);
        String value = strategies.cacheAside("k1", String.class, () -> "db-value", Duration.ofMinutes(5));

        assertThat(value).isEqualTo("db-value");
        verify(client).put("k1", "db-value", Duration.ofMinutes(5));
    }

    @Test
    void writeThroughWritesSourceThenCache() {
        RedisCacheClient client = mock(RedisCacheClient.class);
        RedisCacheStrategies strategies = new RedisCacheStrategies(client);
        AtomicBoolean sourceWritten = new AtomicBoolean(false);

        String result = strategies.writeThrough("k2", "payload", v -> sourceWritten.set(true), Duration.ofSeconds(30));

        assertThat(result).isEqualTo("payload");
        assertThat(sourceWritten.get()).isTrue();
        verify(client).put("k2", "payload", Duration.ofSeconds(30));
    }
}
