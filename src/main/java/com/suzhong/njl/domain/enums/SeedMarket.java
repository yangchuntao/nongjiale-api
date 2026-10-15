package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SeedMarket {
    SUZHONG("SUZHONG", "苏中");

    @EnumValue
    private final String code;
    private final String label;

    SeedMarket(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }
}
