package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 运营后台账号的角色。
 *
 * <p>落在 {@code admin_user.role} 列，存 {@link EnumValue} 标注的 {@code code}。
 * 两种角色都放行 {@code /api/admin/**}（见 {@code SecurityConfig}），当前没有更细的权限切分；
 * 前端展示角色名取 {@link #label()}。
 */
public enum AdminRole {
    /** 运营：可执行审核、开城、代编门店、站点配置等全部后台操作。 */
    OPERATOR("OPERATOR", "运营"),
    /** 客服：同样可进后台，业务上按客服职责使用，权限与运营一致。 */
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
