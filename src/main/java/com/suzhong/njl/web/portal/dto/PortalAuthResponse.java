package com.suzhong.njl.web.portal.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(name = "PortalAuthResponse", description = "游客注册/登录结果，POST /api/auth/register 与 POST /api/auth/login 的响应体；不返回手机号")
public record PortalAuthResponse(
        @Schema(description = "访问令牌，原样放入 Authorization: Bearer 头；只存在于进程内存，不进 Redis、不落库")
        String token,
        @Schema(description = "令牌过期时间，UTC 瞬时点（ISO-8601）")
        Instant expiresAt,
        @Schema(description = "游客用户 ID，即 portal_user.id")
        Long userId,
        @Schema(description = "令牌角色，固定 TOURIST")
        UserType role,
        @Schema(description = "展示名；注册时未填则用脱敏手机号兜底")
        String displayName) {
}
