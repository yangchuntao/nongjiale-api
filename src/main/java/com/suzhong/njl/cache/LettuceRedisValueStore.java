package com.suzhong.njl.cache;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LettuceRedisValueStore implements RedisValueStore {

    private final StringRedisTemplate redis;

    public LettuceRedisValueStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }
}
