package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 站点配置的键名。
 *
 * <p>落在 {@code site_config.config_key} 列（主键），存 {@code @EnumValue} 标注的 {@code code}，
 * 即带点号的配置键字符串。前三个键由 {@code GET/PUT /api/admin/site-config} 读写；
 * 后两个开城阈值键只作为常量被开城校验读取，后台接口不接受修改。
 * 门户 {@code GET /api/config/public} 只暴露站名、主色、客服电话。
 */
public enum SiteConfigKey {
    /** 站点名称，通过 {@code GET /api/config/public} 提供给门户展示。 */
    NAME("site.name"),
    /** 站点主色，十六进制颜色值，例如 {@code #4BA3D9}。 */
    BRAND_COLOR("site.brand_color"),
    /** 客服电话，门户展示用，不参与登录。 */
    CONTACT_PHONE("site.contact_phone"),
    /** 开城门槛：市内存量上架门店的最少数量；只读，供开城校验使用。 */
    OPEN_CITY_MIN_STORES("open_city.min_stores"),
    /** 开城门槛：市内已发布攻略的最少数量；只读，供开城校验使用。 */
    OPEN_CITY_MIN_ARTICLES("open_city.min_articles");

    @EnumValue
    private final String code;

    SiteConfigKey(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
