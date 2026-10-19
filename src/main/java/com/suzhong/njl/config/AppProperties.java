package com.suzhong.njl.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.suzhong.njl.cache.CacheName;

/**
 * 应用自有配置，绑定 {@code app.*} 前缀。
 *
 * <p>由 {@code RedisCacheConfig} 上的 {@code @EnableConfigurationProperties} 注册为 Bean，
 * 供 CORS、令牌有效期和缓存组件读取。所有字段都有与 {@code application.yml} 一致的默认值，
 * 缺少配置时不会启动失败；也可用环境变量覆盖（Spring Boot 松散绑定，如
 * {@code APP_SECURITY_TOKEN_TTL=12h}）。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** CORS 白名单，对应 {@code app.cors-allowed-origins}，环境变量 {@code APP_CORS_ALLOWED_ORIGINS}（逗号分隔）。 */
    private List<String> corsAllowedOrigins = new ArrayList<>();
    /** 安全相关配置，对应 {@code app.security.*}。 */
    private Security security = new Security();
    /** 缓存相关配置，对应 {@code app.cache.*}。 */
    private Cache cache = new Cache();

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /** 安全配置组。 */
    public static class Security {
        /** 内存登录令牌有效期，对应 {@code app.security.token-ttl}，默认 12 小时；过期后需重新登录。 */
        private Duration tokenTtl = Duration.ofHours(12);

        public Duration getTokenTtl() {
            return tokenTtl;
        }

        public void setTokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
        }
    }

    /** 缓存配置组，控制旁路缓存是否启用、键前缀与各业务缓存 TTL。 */
    public static class Cache {
        /** 缓存总开关，对应 {@code app.cache.enabled}；关闭后所有读写直接走数据库，便于排障。 */
        private boolean enabled = true;
        /** Redis 键统一前缀，对应 {@code app.cache.key-prefix}，默认 {@code njl:}，多环境共库时用于隔离。 */
        private String keyPrefix = "njl:";
        /** 未单独配置的缓存使用的兜底 TTL，对应 {@code app.cache.default-ttl}，默认 10 分钟。 */
        private Duration defaultTtl = Duration.ofMinutes(10);
        /** 首页 feed TTL，对应 {@code app.cache.home-feed-ttl}，默认 5 分钟。 */
        private Duration homeFeedTtl = Duration.ofMinutes(5);
        /** 门店详情 TTL，对应 {@code app.cache.store-detail-ttl}，默认 5 分钟。 */
        private Duration storeDetailTtl = Duration.ofMinutes(5);
        /** 地区树 TTL，对应 {@code app.cache.region-ttl}，默认 30 分钟。 */
        private Duration regionTtl = Duration.ofMinutes(30);
        /** 站点配置 TTL，对应 {@code app.cache.site-config-ttl}，默认 30 分钟。 */
        private Duration siteConfigTtl = Duration.ofMinutes(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 按缓存名取 TTL。
         *
         * <p>枚举与配置一一对应，新增 {@code CacheName} 常量时这里必须同步补分支，
         * 否则编译期就会因 switch 不完备而失败（有意如此，避免漏配）。
         *
         * @param name 缓存名
         * @return 该缓存应使用的过期时间
         */
        public Duration ttlFor(CacheName name) {
            return switch (name) {
                case HOME_FEED -> homeFeedTtl;
                case STORE_DETAIL -> storeDetailTtl;
                case REGION -> regionTtl;
                case SITE_CONFIG -> siteConfigTtl;
            };
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getHomeFeedTtl() {
            return homeFeedTtl;
        }

        public void setHomeFeedTtl(Duration homeFeedTtl) {
            this.homeFeedTtl = homeFeedTtl;
        }

        public Duration getStoreDetailTtl() {
            return storeDetailTtl;
        }

        public void setStoreDetailTtl(Duration storeDetailTtl) {
            this.storeDetailTtl = storeDetailTtl;
        }

        public Duration getRegionTtl() {
            return regionTtl;
        }

        public void setRegionTtl(Duration regionTtl) {
            this.regionTtl = regionTtl;
        }

        public Duration getSiteConfigTtl() {
            return siteConfigTtl;
        }

        public void setSiteConfigTtl(Duration siteConfigTtl) {
            this.siteConfigTtl = siteConfigTtl;
        }
    }
}
