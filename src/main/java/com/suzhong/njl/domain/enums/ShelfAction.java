package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ShelfAction {
    PUBLISH("PUBLISH", "申请上架"),
    UNPUBLISH("UNPUBLISH", "申请下架");

    @EnumValue
    private final String code;
    private final String label;

    ShelfAction(String code, String label) {
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
