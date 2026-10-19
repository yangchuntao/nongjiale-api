package com.suzhong.njl.service.admin;

import org.springframework.stereotype.Component;

import com.suzhong.njl.domain.entity.SiteConfig;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.service.city.CityRules;

/**
 * 开城门槛读取器（运营端）。
 *
 * <p>职责：从 {@code site_config} 读取 {@code open_city.min_stores} 与 {@code open_city.min_articles}。
 * 缺行、空串、非数字、0 或负数都回退 {@link CityRules} 的默认值，保证开通校验不会因坏配置抛 500。
 *
 * <p>不做缓存：每次开城校验和详情读取都回源，改配置后立即生效。门槛只读，
 * 后台站点配置接口不接受修改这两个键。
 */
@Component
public class OpenCityThresholds {

    private final SiteConfigMapper siteConfigMapper;

    public OpenCityThresholds(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    /**
     * 生效的最低上架门店数 N。
     *
     * @return 配置值；缺失或非法时回退 {@link CityRules#DEFAULT_MIN_STORES}
     */
    public int minStores() {
        return CityRules.positiveOrDefault(read(SiteConfigKey.OPEN_CITY_MIN_STORES), CityRules.DEFAULT_MIN_STORES);
    }

    /**
     * 生效的最低已发布攻略数 M。
     *
     * @return 配置值；缺失或非法时回退 {@link CityRules#DEFAULT_MIN_ARTICLES}
     */
    public int minArticles() {
        return CityRules.positiveOrDefault(read(SiteConfigKey.OPEN_CITY_MIN_ARTICLES), CityRules.DEFAULT_MIN_ARTICLES);
    }

    /** 按配置键读原始字符串，缺行返回 null；解析与兜底交给 CityRules。 */
    private String read(SiteConfigKey key) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        return row == null ? null : row.getConfigValue();
    }
}
