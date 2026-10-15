package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum MerchantOrgStatus {
    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "停用");

    @EnumValue
    private final String code;
    private final String label;

    MerchantOrgStatus(String code, String label) {
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
