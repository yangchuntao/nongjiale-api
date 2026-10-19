package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 商家主体的启用状态。
 *
 * <p>落在 {@code merchant_org.status} 列，存 {@code @EnumValue} 标注的 {@code code}；
 * 商家账号另有 {@code merchant_user.enabled} 布尔开关，两者任一不可用都会导致登录 403。
 * 状态由运营在 {@code PATCH /api/admin/merchants/{orgId}} 切换，停用后该主体旧令牌失效。
 * 前端展示中文取 {@link #label()}。
 */
public enum MerchantOrgStatus {
    /** 启用：主体可登录商家端并维护本主体门店。 */
    ACTIVE("ACTIVE", "启用"),
    /** 停用：禁止登录，既有令牌失效；门店与历史数据保留。 */
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
