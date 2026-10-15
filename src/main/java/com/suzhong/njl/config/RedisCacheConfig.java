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

@Configuration
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
public class RedisCacheConfig {

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

    private RedisCacheConfiguration base(String keyPrefix) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(keyPrefix)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
