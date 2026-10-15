package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum RegionLevel {
    PROVINCE(1, "省"),
    CITY(2, "市"),
    DISTRICT(3, "区县");

    @EnumValue
    private final int code;
    private final String label;

    RegionLevel(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static RegionLevel fromCode(int code) {
        for (RegionLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知地区层级: " + code);
    }
}
