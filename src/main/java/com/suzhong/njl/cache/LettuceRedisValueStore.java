package com.suzhong.njl.cache;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * {@code RedisValueStore} 的 Redis 实现，基于 Spring Data Redis 的 {@code StringRedisTemplate}。
 *
 * <p>使用字符串模板意味着键和值都按 UTF-8 字符串收发，JSON 由 {@code CacheAside} 用 ObjectMapper
 * 处理；连接由 Lettuce 管理，超时时间来自 {@code spring.data.redis.timeout/connect-timeout}。
 * 不做异常捕获——连接失败会抛 {@code DataAccessException}，交由上层一次性降级为直接读库。
 */
@Component
public class LettuceRedisValueStore implements RedisValueStore {

    private final StringRedisTemplate redis;

    public LettuceRedisValueStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 读取字符串；键不存在返回 null。 */
    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    /** 写入并设置 TTL，依赖 Redis 的 SETEX 语义，避免出现永不过期的键。 */
    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    /** 删除键；键不存在不报错。 */
    @Override
    public void delete(String key) {
        redis.delete(key);
    }
}
