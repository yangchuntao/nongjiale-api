package com.suzhong.njl.service.portal;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.SiteConfig;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.web.portal.dto.PublicConfigResponse;

/**
 * 门户站点配置查询服务。
 *
 * <p>职责：给 {@code GET /api/config/public} 提供站名、主色、客服电话，供门户渲染品牌与「联系客服」。
 * 客服电话不随门店下架/关城变化，未开通门店仍使用这里的站点电话。
 *
 * <p>缓存：键 {@code site-config:public}，TTL 见 {@code app.cache.site-config-ttl}（默认 30 分钟）；
 * 运营改站点配置后需删除该键。缺行或配置值为 null 时返回空串，前端按「未配置」处理。
 *
 * <p>只读，无事务；Redis 不可用时直接查库。
 */
@Service
public class PublicConfigService {

    private static final TypeReference<PublicConfigResponse> TYPE = new TypeReference<>() {
    };

    private final SiteConfigMapper siteConfigMapper;
    private final CacheAside cacheAside;

    public PublicConfigService(SiteConfigMapper siteConfigMapper, CacheAside cacheAside) {
        this.siteConfigMapper = siteConfigMapper;
        this.cacheAside = cacheAside;
    }

    /**
     * 查询公开站点配置。
     *
     * @return 站名、主色、客服电话；未配置的项为空串而不是 null
     */
    public PublicConfigResponse publicConfig() {
        return cacheAside.get(CacheName.SITE_CONFIG, "public", TYPE, this::load);
    }

    /** 依次读取三个固定配置键并组装响应；键值以枚举 code 为主键查询。 */
    private PublicConfigResponse load() {
        return new PublicConfigResponse(
                value(SiteConfigKey.NAME),
                value(SiteConfigKey.BRAND_COLOR),
                value(SiteConfigKey.CONTACT_PHONE));
    }

    /** 读单个配置值：缺行或值为 null 时统一回空串，避免前端拿到 null 后渲染出 "null"。 */
    private String value(SiteConfigKey key) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        return row == null || row.getConfigValue() == null ? "" : row.getConfigValue();
    }
}
