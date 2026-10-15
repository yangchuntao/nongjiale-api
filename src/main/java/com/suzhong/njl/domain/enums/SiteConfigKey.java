package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SiteConfigKey {
    NAME("site.name"),
    BRAND_COLOR("site.brand_color"),
    CONTACT_PHONE("site.contact_phone");

    @EnumValue
    private final String code;

    SiteConfigKey(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
