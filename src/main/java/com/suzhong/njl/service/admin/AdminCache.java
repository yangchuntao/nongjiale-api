package com.suzhong.njl.service.admin;

import org.springframework.stereotype.Component;

import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;

/**
 * 运营写路径的缓存失效收口（运营端）。
 *
 * <p>职责：把运营操作影响到的热点缓存键集中在本类，避免各服务各写一遍键名。
 * 业务落库成功后调用；缓存关闭或 Redis 不可用时由 {@link CacheAside} 直接读库。
 *
 * <p>关键约束：
 * <ul>
 *   <li>删除失败只记日志，不抛异常、不回滚已经提交的数据库事务；</li>
 *   <li>门店写操作同时失效门店详情与首页聚合，避免首页仍展示旧文案；</li>
 *   <li>开城/关城才失效地区树与首页；checklist 手勾不影响对客，不调用。</li>
 * </ul>
 */
@Component
public class AdminCache {

    private final CacheAside cacheAside;

    public AdminCache(CacheAside cacheAside) {
        this.cacheAside = cacheAside;
    }

    /**
     * 门店写操作后的失效：门店详情按 ID 删，首页聚合固定删 {@code default} 键。
     * 门店为 null 时只删首页，用于兜底调用；正常写路径都会带上门店 ID。
     */
    public void evictStore(Long storeId) {
        if (storeId != null) {
            cacheAside.evict(CacheName.STORE_DETAIL, String.valueOf(storeId));
        }
        cacheAside.evict(CacheName.HOME_FEED, "default");
    }

    /** 攻略发布、修改、删除后只需失效首页聚合，攻略详情走库不缓存。 */
    public void evictArticles() {
        cacheAside.evict(CacheName.HOME_FEED, "default");
    }

    /** 站点配置保存后失效公开配置缓存，键固定为 {@code public}。 */
    public void evictPublicConfig() {
        cacheAside.evict(CacheName.SITE_CONFIG, "public");
    }

    /** 开通或关城后失效地区树和首页。手勾不调用。 */
    public void evictCityOpen() {
        cacheAside.evict(CacheName.REGION, "enabled");
        cacheAside.evict(CacheName.HOME_FEED, "default");
    }
}
