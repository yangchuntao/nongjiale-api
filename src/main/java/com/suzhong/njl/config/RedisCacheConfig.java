package com.suzhong.njl.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.suzhong.njl.cache.CacheName;

/**
 * Redis 缓存与配置属性注册。
 *
 * <p>做三件事：开启 Spring Cache 注解、注册 {@code AppProperties} 与
 * {@code WechatMiniappProperties}、定义带统一前缀与 JSON 序列化的 {@code RedisCacheManager}。
 *
 * <p>注意：当前业务读写走手写的 {@code CacheAside}（{@code StringRedisTemplate} + ObjectMapper），
 * 这里的 Manager 主要给后续 {@code @Cacheable} 使用。两者前缀相同但键格式不同
 * （Spring Cache 用双冒号分隔 cache 名与键），不会互相覆盖，但仍不建议对同一份数据混用两套。
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties({AppProperties.class, WechatMiniappProperties.class})
public class RedisCacheConfig {

    /**
     * 产出 Redis 缓存管理器。
     *
     * <p>默认配置使用 {@code app.cache.default-ttl}（10 分钟），再为四个业务缓存覆盖独立 TTL：
     * 首页 feed 与门店详情 5 分钟、地区与站点配置 30 分钟，对应 {@code app.cache.*-ttl}。
     * 值用 {@code GenericJackson2JsonRedisSerializer}，JSON 中带类型信息，反序列化能还原原对象；
     * 键统一加 {@code app.cache.key-prefix}，多环境共用实例时可区分。
     *
     * @param connectionFactory Redis 连接工厂
     * @param appProperties     缓存前缀与各档 TTL 来源
     * @return 已注册每缓存独立 TTL 的缓存管理器
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory, AppProperties appProperties) {
        AppProperties.Cache cache = appProperties.getCache();
        RedisCacheConfiguration defaults = base(cache.getKeyPrefix()).entryTtl(cache.getDefaultTtl());
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CacheName.HOME_FEED.key(), defaults.entryTtl(cache.getHomeFeedTtl()));
        perCache.put(CacheName.STORE_DETAIL.key(), defaults.entryTtl(cache.getStoreDetailTtl()));
        perCache.put(CacheName.REGION.key(), defaults.entryTtl(cache.getRegionTtl()));
        perCache.put(CacheName.SITE_CONFIG.key(), defaults.entryTtl(cache.getSiteConfigTtl()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /** 公共基础配置：键前缀 + JSON 值序列化，TTL 由调用方追加。 */
    private RedisCacheConfiguration base(String keyPrefix) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(keyPrefix)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
