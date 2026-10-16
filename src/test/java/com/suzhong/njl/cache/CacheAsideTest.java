package com.suzhong.njl.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

import com.fasterxml.jackson.core.JsonProcessingException;
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

            @Override
            public void delete(String key) {
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

            @Override
            public void delete(String key) {
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

    @Test
    void hitReturnsParsedValueWithoutCallingLoader() {
        AtomicInteger loads = new AtomicInteger();
        RedisValueStore redis = recordingRedis("\"cached\"");
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());

        String value = cacheAside.get(CacheName.HOME_FEED, "default", new TypeReference<>() {
        }, () -> {
            loads.incrementAndGet();
            return "from-db";
        });

        assertThat(value).isEqualTo("cached");
        assertThat(loads).hasValue(0);
    }

    @Test
    void blankOrBrokenCacheFallsThroughAndWritesTheLoadedValue() {
        AtomicInteger sets = new AtomicInteger();
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                return "   ";
            }

            @Override
            public void set(String key, String value, Duration ttl) {
                sets.incrementAndGet();
                assertThat(key).isEqualTo("njl:region:enabled");
                assertThat(value).isEqualTo("\"db\"");
                assertThat(ttl).isEqualTo(Duration.ofMinutes(30));
            }

            @Override
            public void delete(String key) {
            }
        };
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());

        assertThat(cacheAside.get(CacheName.REGION, "enabled", new TypeReference<String>() {
        }, () -> "db")).isEqualTo("db");
        assertThat(sets).hasValue(1);

        RedisValueStore broken = recordingRedis("not-json");
        CacheAside again = new CacheAside(broken, new ObjectMapper(), enabled());
        assertThat(again.get(CacheName.STORE_DETAIL, "1", new TypeReference<String>() {
        }, () -> "fresh")).isEqualTo("fresh");
    }

    @Test
    void nullLoadIsNotCached() {
        AtomicInteger sets = new AtomicInteger();
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void set(String key, String value, Duration ttl) {
                sets.incrementAndGet();
            }

            @Override
            public void delete(String key) {
            }
        };
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());

        assertThat(cacheAside.get(CacheName.SITE_CONFIG, "public", new TypeReference<String>() {
        }, () -> null)).isNull();
        assertThat(sets).hasValue(0);
    }

    @Test
    void nonInfrastructureErrorsPropagateAndSerializationFailureStillReturnsTheValue() throws Exception {
        RedisValueStore exploding = new RedisValueStore() {
            @Override
            public String get(String key) {
                throw new IllegalStateException("bug");
            }

            @Override
            public void set(String key, String value, Duration ttl) {
            }

            @Override
            public void delete(String key) {
            }
        };
        CacheAside cacheAside = new CacheAside(exploding, new ObjectMapper(), enabled());
        assertThatThrownBy(() -> cacheAside.get(CacheName.HOME_FEED, "k", new TypeReference<String>() {
        }, () -> "db")).isInstanceOf(IllegalStateException.class);

        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("nope") {
        });
        RedisValueStore redis = recordingRedis(null);
        CacheAside serializing = new CacheAside(redis, mapper, enabled());
        assertThat(serializing.get(CacheName.HOME_FEED, "k", new TypeReference<String>() {
        }, () -> "db")).isEqualTo("db");
    }

    @Test
    void writeFailureThatIsNotInfrastructureIsRethrown() {
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void set(String key, String value, Duration ttl) {
                throw new IllegalStateException("write");
            }

            @Override
            public void delete(String key) {
            }
        };
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());
        assertThatThrownBy(() -> cacheAside.get(CacheName.HOME_FEED, "k", new TypeReference<String>() {
        }, () -> "db")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nestedInfrastructureFailureOnReadMarksRedisDown() {
        AtomicInteger reads = new AtomicInteger();
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                reads.incrementAndGet();
                throw new IllegalStateException("wrapped", new RedisConnectionFailureException("down"));
            }

            @Override
            public void set(String key, String value, Duration ttl) {
            }

            @Override
            public void delete(String key) {
            }
        };
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());
        assertThat(cacheAside.get(CacheName.SITE_CONFIG, "k", new TypeReference<String>() {
        }, () -> "db")).isEqualTo("db");
        assertThat(cacheAside.get(CacheName.SITE_CONFIG, "k", new TypeReference<String>() {
        }, () -> "db2")).isEqualTo("db2");
        assertThat(reads).hasValue(1);
    }

    @Test
    void evictDeletesWhenRedisIsUpAndSwallowsDeleteErrors() {
        AtomicInteger deletes = new AtomicInteger();
        RedisValueStore redis = new RedisValueStore() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void set(String key, String value, Duration ttl) {
            }

            @Override
            public void delete(String key) {
                deletes.incrementAndGet();
                assertThat(key).isEqualTo("njl:home-feed:default");
                if (deletes.get() == 2) {
                    throw new IllegalStateException("delete");
                }
            }
        };
        CacheAside cacheAside = new CacheAside(redis, new ObjectMapper(), enabled());
        cacheAside.evict(CacheName.HOME_FEED, "default");
        cacheAside.evict(CacheName.HOME_FEED, "default");
        assertThat(deletes).hasValue(2);

        AppProperties disabled = enabled();
        disabled.getCache().setEnabled(false);
        new CacheAside(redis, new ObjectMapper(), disabled).evict(CacheName.HOME_FEED, "default");
        assertThat(deletes).hasValue(2);
    }

    private RedisValueStore recordingRedis(String raw) {
        return new RedisValueStore() {
            @Override
            public String get(String key) {
                return raw;
            }

            @Override
            public void set(String key, String value, Duration ttl) {
            }

            @Override
            public void delete(String key) {
            }
        };
    }

    private AppProperties enabled() {
        AppProperties properties = new AppProperties();
        properties.getCache().setEnabled(true);
        properties.getCache().setKeyPrefix("njl:");
        return properties;
    }
}
