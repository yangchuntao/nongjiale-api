package com.suzhong.njl.web.admin.dto;

import com.suzhong.njl.domain.enums.AdminRole;

/**
 * 当前运营身份（{@code GET /api/admin/auth/me} 的 data）。
 *
 * <p>用于前端刷新页面后确认令牌仍然有效并渲染顶栏身份；未登录或令牌已吊销返回 401。
 *
 * @param adminId     运营账号 ID，对应 {@code admin_user.id}
 * @param phone       登录手机号，不脱敏
 * @param role        角色码，取值见 {@link AdminRole}：OPERATOR（运营）、AGENT（客服）
 * @param displayName 账号展示名，非空
 */
public record AdminMeResponse(
        Long adminId,
        String phone,
        AdminRole role,
        String displayName) {
}
