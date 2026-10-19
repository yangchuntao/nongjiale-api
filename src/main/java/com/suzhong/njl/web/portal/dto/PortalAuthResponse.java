package com.suzhong.njl.web.portal.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

/**
 * 游客注册/登录结果（{@code POST /api/auth/register} 与 {@code POST /api/auth/login} 的响应体）。
 *
 * <p>令牌是进程内存令牌，请求时放在 {@code Authorization: Bearer <token>}；角色固定 {@code TOURIST}。
 * 本结构不返回手机号，手机号需另调 {@code GET /api/auth/me}。
 *
 * @param token       访问令牌，原样放入 Bearer 头；令牌只存在于进程内存，不进 Redis、不落库
 * @param expiresAt   令牌过期时间，UTC 瞬时点（ISO-8601）
 * @param userId      游客用户 ID，即 {@code portal_user.id}
 * @param role        令牌角色，固定 {@link com.suzhong.njl.domain.enums.UserType#TOURIST}
 * @param displayName 展示名；注册时未填则用脱敏手机号兜底
 */
public record PortalAuthResponse(
        String token,
        Instant expiresAt,
        Long userId,
        UserType role,
        String displayName) {
}
