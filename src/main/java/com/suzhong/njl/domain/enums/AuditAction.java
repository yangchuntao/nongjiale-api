package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AuditAction {
    APPLY_APPROVE("APPLY_APPROVE", "入驻通过"),
    APPLY_REJECT("APPLY_REJECT", "入驻驳回"),
    SHELF_FORCE("SHELF_FORCE", "代上下架"),
    SHELF_REVIEW("SHELF_REVIEW", "审上下架"),
    INTENT_STATUS("INTENT_STATUS", "意向状态"),
    SITE_CONFIG_UPDATE("SITE_CONFIG_UPDATE", "站点配置"),
    MERCHANT_DISABLE("MERCHANT_DISABLE", "商家停用"),
    MERCHANT_ENABLE("MERCHANT_ENABLE", "商家启用"),
    CITY_CHECKLIST("CITY_CHECKLIST", "开城清单"),
    CITY_OPEN("CITY_OPEN", "开通城市"),
    CITY_CLOSE("CITY_CLOSE", "关闭城市"),
    BANNER_CREATE("BANNER_CREATE", "新建 Banner"),
    BANNER_UPDATE("BANNER_UPDATE", "更新 Banner"),
    BANNER_ENABLE("BANNER_ENABLE", "启用 Banner"),
    BANNER_DISABLE("BANNER_DISABLE", "停用 Banner"),
    BANNER_DELETE("BANNER_DELETE", "删除 Banner");

    @EnumValue
    private final String code;
    private final String label;

    AuditAction(String code, String label) {
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
