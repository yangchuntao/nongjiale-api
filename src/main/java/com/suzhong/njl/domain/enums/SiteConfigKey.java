package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SiteConfigKey {
    NAME("site.name"),
    BRAND_COLOR("site.brand_color"),
    CONTACT_PHONE("site.contact_phone"),
    OPEN_CITY_MIN_STORES("open_city.min_stores"),
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
