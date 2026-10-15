package com.suzhong.njl.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.config.AppProperties;

class CacheAsideTest {

    @Test
    void redisFailureFallsBackToDatabaseAndStopsCallingRedis() {
        AtomicInteger reads = new AtomicInteger();
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                reads.incrementAndGet();
                throw new RedisConnectionFailureException("down");
            }

            @Override
            public void set(String key, String value, Duration ttl) {
                throw new RedisConnectionFailureException("down");
            }
        };
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());

        String first = cacheAside.get(CacheName.SITE_CONFIG, "public", new TypeReference<>() {
        }, () -> "from-db");
        String second = cacheAside.get(CacheName.SITE_CONFIG, "public", new TypeReference<>() {
        }, () -> "from-db-again");

        assertThat(first).isEqualTo("from-db");
        assertThat(second).isEqualTo("from-db-again");
        assertThat(reads).hasValue(1);
    }

    @Test
    void disabledCacheDoesNotTouchRedis() {
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                throw new AssertionError("不应访问 Redis");
            }

            @Override
            public void set(String key, String value, Duration ttl) {
                throw new AssertionError("不应访问 Redis");
            }
        };
        AppProperties properties = enabled();
        properties.getCache().setEnabled(false);
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), properties);

        String value = cacheAside.get(CacheName.REGION, "enabled", new TypeReference<>() {
        }, () -> "db");

        assertThat(value).isEqualTo("db");
    }

    private AppProperties enabled() {
        AppProperties properties = new AppProperties();
        properties.getCache().setEnabled(true);
        properties.getCache().setKeyPrefix("njl:");
        return properties;
    }
}
