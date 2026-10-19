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
 * 热点读的旁路缓存（cache-aside）组件。
 *
 * <p>读流程：按「键前缀 + 缓存名 + 业务键」查 Redis，命中则反序列化返回；未命中、内容为空或
 * 无法解析时调用 loader 回源，并按该缓存名的 TTL 回填。写流程由业务方在数据库提交后调用
 * {@link #evict}，即「先写库、再删缓存」，下一次读自然回源，避免双写顺序问题。
 *
 * <p>并发与降级：没有分布式锁，缓存击穿时允许多个请求同时回源，依靠数据库唯一约束与幂等写兜底；
 * Redis 出现基础设施异常时把进程内开关置为不可用，后续请求直接读库，避免每个请求都等
 * 连接超时。该开关是单向的，Redis 恢复后需重启进程才会重新使用缓存。
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

    /**
     * 读缓存，未命中时回源并回填。
     *
     * @param cache  缓存名，决定键段与 TTL 档位
     * @param key    业务键，如门店 ID、{@code default}、{@code enabled}
     * @param type   反序列化目标类型；嵌套泛型必须用 TypeReference 保留类型信息
     * @param loader 回源函数，返回 null 时不写缓存
     * @param <T>    返回类型
     * @return 缓存内容或回源结果；loader 返回 null 时同样返回 null
     */
    public <T> T get(CacheName cache, String key, TypeReference<T> type, Supplier<T> loader) {
        // 缓存开关关闭或 Redis 已判定不可用时，直接读库；缓存故障不能变成 5xx
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
                    // 结构变更或脏数据时按未命中处理，不能让反序列化异常冒泡成 500
                    log.warn("缓存内容无法解析，改为读库 cache={}", cache.key());
                }
            }
        } catch (RuntimeException ex) {
            if (isInfrastructure(ex)) {
                markDown(ex);
                return loader.get();
            }
            // 非连接类异常（多为代码问题）继续抛出，避免把 bug 藏起来
            throw ex;
        }
        T loaded = loader.get();
        if (loaded == null) {
            // 空结果不落缓存，避免把「暂时查不到」固化到 TTL 结束
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
            // 序列化失败只影响缓存，本次仍返回已查到的数据
            log.warn("缓存写入序列化失败 cache={}", cache.key());
        }
        return loaded;
    }

    /**
     * 写路径失效：删除对应缓存键。
     *
     * <p>删除失败只记日志，不回滚已经成功的数据库写入；下次读到的旧值会在 TTL 到期后自然修正。
     *
     * @param cache 缓存名
     * @param key   业务键，必须与读路径使用同一个键
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

    /** 标记 Redis 不可用；compareAndSet 保证并发失败时只打一条告警。 */
    private void markDown(RuntimeException ex) {
        if (redisUp.compareAndSet(true, false)) {
            log.warn("Redis 不可用，热点读改为直接读库", ex);
        }
    }

    /** 沿 cause 链判断是否为 Spring 数据访问异常（连接失败、超时等），只有这类异常才降级。 */
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
