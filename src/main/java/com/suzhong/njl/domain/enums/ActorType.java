package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ActorType {
    ADMIN("ADMIN", "运营"),
    MERCHANT("MERCHANT", "商家"),
    SYSTEM("SYSTEM", "系统");

    @EnumValue
    private final String code;
    private final String label;

    ActorType(String code, String label) {
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
