package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.UserType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前游客身份（{@code GET /api/auth/me} 的响应体，须游客 Bearer）。
 *
 * <p>手机号按库中原样返回，<b>不脱敏</b>；微信壳账号尚未打通手机号时该字段为 null。
 * 本结构不返回 {@code session_key}、密码哈希或微信 openid。
 *
 * @param userId      游客用户 ID
 * @param phone       手机号，明文不脱敏，可空（未绑定手机号的微信壳账号）
 * @param displayName 展示名；可能为脱敏手机号或「微信用户」
 * @param role        令牌角色，固定 {@link com.suzhong.njl.domain.enums.UserType#TOURIST}
 */
@Schema(name = "PortalMeResponse", description = "当前游客身份，GET /api/auth/me 的响应体，须游客 Bearer；不返回 session_key、密码哈希或微信 openid")
public record PortalMeResponse(
        @Schema(description = "游客用户 ID")
        Long userId,
        @Schema(description = "手机号，明文不脱敏，可空（未绑定手机号的微信壳账号）")
        String phone,
        @Schema(description = "展示名；可能为脱敏手机号或「微信用户」")
        String displayName,
        @Schema(description = "令牌角色，固定 TOURIST")
        UserType role) {
}
