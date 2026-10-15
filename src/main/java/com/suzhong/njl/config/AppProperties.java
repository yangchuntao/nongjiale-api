package com.suzhong.njl.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private List<String> corsAllowedOrigins = new ArrayList<>();
    private Security security = new Security();
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

    public static class Security {
        private Duration tokenTtl = Duration.ofHours(12);

        public Duration getTokenTtl() {
            return tokenTtl;
        }

        public void setTokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
        }
    }

    public static class Cache {
        private String keyPrefix = "njl:";
        private Duration defaultTtl = Duration.ofMinutes(10);
        private Duration homeFeedTtl = Duration.ofMinutes(5);
        private Duration storeDetailTtl = Duration.ofMinutes(5);
        private Duration regionTtl = Duration.ofMinutes(30);
        private Duration siteConfigTtl = Duration.ofMinutes(30);

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
