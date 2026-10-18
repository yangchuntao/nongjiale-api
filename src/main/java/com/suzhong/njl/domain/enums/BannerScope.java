package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum BannerScope {
    ALL("ALL"),
    CITY("CITY");

    @EnumValue
    private final String code;

    BannerScope(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
