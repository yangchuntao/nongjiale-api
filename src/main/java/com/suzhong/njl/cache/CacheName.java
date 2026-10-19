package com.suzhong.njl.cache;

/**
 * 缓存名枚举，同时充当 Redis 键的命名空间与 TTL 档位索引。
 *
 * <p>完整键由 {@code CacheAside} 拼成「{@code app.cache.key-prefix} + 本枚举 key + 业务键」，
 * 例如 {@code njl:store-detail:123}。TTL 不写在本枚举里，而是由
 * {@code AppProperties.Cache.ttlFor} 按枚举映射到 {@code app.cache.*-ttl}，改配置即可调整。
 */
public enum CacheName {

    /** 首页推荐 feed，业务键固定为 {@code default}；默认 TTL 5 分钟。 */
    HOME_FEED("home-feed"),
    /** 门店详情，业务键为门店 ID；默认 TTL 5 分钟，门店信息变更时主动删除。 */
    STORE_DETAIL("store-detail"),
    /** 已开通地区树，业务键固定为 {@code enabled}；默认 TTL 30 分钟，开城/关城时删除。 */
    REGION("region"),
    /** 公开站点配置，业务键固定为 {@code public}；默认 TTL 30 分钟，保存配置时删除。 */
    SITE_CONFIG("site-config");

    private final String key;

    CacheName(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
