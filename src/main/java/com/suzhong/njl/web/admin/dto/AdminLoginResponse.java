package com.suzhong.njl.web.admin.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.AdminRole;

/**
 * 运营登录响应（{@code POST /api/admin/auth/login} 的 data）。
 *
 * <p>令牌是 Bearer 形式的访问令牌，后续请求放在 {@code Authorization} 头；
 * 角色只区分 OPERATOR 与 AGENT，两者都能访问全部 {@code /api/admin/**}。
 *
 * @param token       访问令牌原文，32 字节随机数的十六进制串，不透明；只在本次响应返回，前端自行保存
 * @param expiresAt   令牌过期时间，UTC 的 ISO-8601 时间点（如 {@code 2026-10-19T10:00:00Z}）
 * @param adminId     运营账号 ID，对应 {@code admin_user.id}
 * @param role        角色码，取值见 {@link AdminRole}：OPERATOR（运营）、AGENT（客服）
 * @param displayName 账号展示名，非空，用于后台顶栏显示
 */
public record AdminLoginResponse(
        String token,
        Instant expiresAt,
        Long adminId,
        AdminRole role,
        String displayName) {
}
