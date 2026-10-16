package com.suzhong.njl.cache;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.config.AppProperties;

/**
 * 热点读 cache-aside。Redis 不可用时直接读库，不把缓存故障变成 5xx。
 * 令牌仍在进程内存，不写入本组件。
 */
@Component
public class CacheAside {

    private static final Logger log = LoggerFactory.getLogger(CacheAside.class);

    private final RedisValueStore redis;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final AtomicBoolean redisUp = new AtomicBoolean(true);

    public CacheAside(RedisValueStore redis, ObjectMapper objectMapper, AppProperties appProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public <T> T get(CacheName cache, String key, TypeReference<T> type, Supplier<T> loader) {
        if (!appProperties.getCache().isEnabled() || !redisUp.get()) {
            return loader.get();
        }
        String redisKey = appProperties.getCache().getKeyPrefix() + cache.key() + ":" + key;
        try {
            String raw = redis.get(redisKey);
            if (raw != null && !raw.isBlank()) {
                try {
                    return objectMapper.readValue(raw, objectMapper.constructType(type));
                } catch (Exception parseError) {
                    log.warn("缓存内容无法解析，改为读库 cache={}", cache.key());
                }
            }
        } catch (RuntimeException ex) {
            if (isInfrastructure(ex)) {
                markDown(ex);
                return loader.get();
            }
            throw ex;
        }
        T loaded = loader.get();
        if (loaded == null) {
            return null;
        }
        try {
            redis.set(redisKey, objectMapper.writeValueAsString(loaded), appProperties.getCache().ttlFor(cache));
        } catch (RuntimeException ex) {
            if (isInfrastructure(ex)) {
                markDown(ex);
            } else {
                throw ex;
            }
        } catch (Exception ex) {
            log.warn("缓存写入序列化失败 cache={}", cache.key());
        }
        return loaded;
    }

    /**
     * 写路径失效。删缓存失败只记日志，不回滚已经成功的数据库写入。
     */
    public void evict(CacheName cache, String key) {
        if (!appProperties.getCache().isEnabled() || !redisUp.get()) {
            return;
        }
        String redisKey = appProperties.getCache().getKeyPrefix() + cache.key() + ":" + key;
        try {
            redis.delete(redisKey);
        } catch (RuntimeException ex) {
            log.warn("缓存失效失败 cache={}", cache.key(), ex);
        }
    }

    private void markDown(RuntimeException ex) {
        if (redisUp.compareAndSet(true, false)) {
            log.warn("Redis 不可用，热点读改为直接读库", ex);
        }
    }

    private boolean isInfrastructure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof DataAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
