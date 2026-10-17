package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 市的对客开通态。只对 {@link RegionLevel#CITY} 有意义。
 * 与意向状态 {@link IntentStatus#CLOSED} 不是同一个枚举。
 */
public enum RegionOpenStatus {
    CLOSED("CLOSED"),
    OPEN("OPEN");

    @EnumValue
    private final String code;

    RegionOpenStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
