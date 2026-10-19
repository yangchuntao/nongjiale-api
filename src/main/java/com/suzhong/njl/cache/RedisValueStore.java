package com.suzhong.njl.cache;

import java.time.Duration;

/**
 * 热点读缓存的字符串存取抽象。令牌不走这里。
 *
 * <p>契约：
 * <ul>
 *   <li>只处理已经序列化好的字符串，不关心 JSON 结构，序列化由调用方负责；</li>
 *   <li>不吞异常，连接失败、超时等原样抛出，由 {@code CacheAside} 判断是否属于基础设施故障并降级；</li>
 *   <li>不实现重试、不做过期兜底，写入必须显式传 TTL。</li>
 * </ul>
 *
 * <p>抽出接口是为了让单测能替换成内存实现，生产实现见 {@code LettuceRedisValueStore}。
 */
public interface RedisValueStore {

    /**
     * 读取字符串值。
     *
     * @param key 完整 Redis 键
     * @return 值；键不存在时返回 null
     */
    String get(String key);

    /**
     * 写入字符串值并设置过期时间。
     *
     * @param key   完整 Redis 键
     * @param value 已序列化的内容
     * @param ttl   过期时间，必须为正数
     */
    void set(String key, String value, Duration ttl);

    /**
     * 删除键，键不存在时视为成功。
     *
     * @param key 完整 Redis 键
     */
    void delete(String key);
}
