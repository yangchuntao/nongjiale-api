package com.suzhong.njl.service.admin;

import org.springframework.stereotype.Component;

import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;

/**
 * 运营写路径失效已有热点缓存。删失败只记日志，不回滚数据库。
 */
@Component
public class AdminCache {

    private final CacheAside cacheAside;

    public AdminCache(CacheAside cacheAside) {
        this.cacheAside = cacheAside;
    }

    public void evictStore(Long storeId) {
        if (storeId != null) {
            cacheAside.evict(CacheName.STORE_DETAIL, String.valueOf(storeId));
        }
        cacheAside.evict(CacheName.HOME_FEED, "default");
    }

    public void evictArticles() {
        cacheAside.evict(CacheName.HOME_FEED, "default");
    }

    public void evictPublicConfig() {
        cacheAside.evict(CacheName.SITE_CONFIG, "public");
    }

    /** 开通或关城后失效地区树和首页。手勾不调用。 */
    public void evictCityOpen() {
        cacheAside.evict(CacheName.REGION, "enabled");
        cacheAside.evict(CacheName.HOME_FEED, "default");
    }
}
