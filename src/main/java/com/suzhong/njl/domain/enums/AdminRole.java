package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AdminRole {
    OPERATOR("OPERATOR", "运营"),
    AGENT("AGENT", "客服");

    @EnumValue
    private final String code;
    private final String label;

    AdminRole(String code, String label) {
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
