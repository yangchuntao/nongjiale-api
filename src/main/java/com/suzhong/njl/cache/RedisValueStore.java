package com.suzhong.njl.cache;

import java.time.Duration;

/**
 * 热点读缓存的字符串存取。令牌不走这里。
 */
public interface RedisValueStore {

    String get(String key);

    void set(String key, String value, Duration ttl);

    void delete(String key);
}
