package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ShelfStatus {
    ON_SHELF("ON_SHELF", "上架"),
    OFF_SHELF("OFF_SHELF", "下架"),
    DRAFT("DRAFT", "草稿");

    @EnumValue
    private final String code;
    private final String label;

    ShelfStatus(String code, String label) {
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
