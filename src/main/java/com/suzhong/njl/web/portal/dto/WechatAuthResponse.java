package com.suzhong.njl.web.portal.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 微信登录/绑定结果（{@code POST /api/auth/wechat/login} 与 {@code POST /api/auth/wechat/bind} 的响应体）。
 *
 * <p>同一 openid 再次登录复用原来的 {@code userId}，不重复建壳用户；绑定成功可能换发目标账号的令牌。
 * 响应<b>不包含</b> {@code session_key}、微信 access_token 或 openid。
 *
 * @param token       访问令牌，放入 {@code Authorization: Bearer <token>}
 * @param expiresAt   令牌过期时间，UTC 瞬时点（ISO-8601）
 * @param userId      绑定/登录后的目标游客用户 ID
 * @param role        令牌角色，固定 {@link com.suzhong.njl.domain.enums.UserType#TOURIST}
 * @param displayName 展示名；未打通的壳账号为「微信用户」
 * @param phone       手机号，明文不脱敏，可空；未绑定手机号时为 null，不补空串
 * @param phoneBound  是否已绑定手机号（{@code portal_user.phone} 非空）
 * @param wechatBound 当前 AppID 下是否已绑定微信身份
 * @param accountLinked 是否完成账号打通：手机号已绑、密码已设、微信已绑三者同时成立才为 true
 */
@Schema(name = "WechatAuthResponse", description = "微信登录/绑定结果，POST /api/auth/wechat/login 与 POST /api/auth/wechat/bind 的响应体；不包含 session_key、微信 access_token 或 openid")
public record WechatAuthResponse(
        @Schema(description = "访问令牌，放入 Authorization: Bearer 头")
        String token,
        @Schema(description = "令牌过期时间，UTC 瞬时点（ISO-8601）")
        Instant expiresAt,
        @Schema(description = "绑定/登录后的目标游客用户 ID；同一 openid 再次登录复用原 userId")
        Long userId,
        @Schema(description = "令牌角色，固定 TOURIST")
        UserType role,
        @Schema(description = "展示名；未打通的壳账号为「微信用户」")
        String displayName,
        @Schema(description = "手机号，明文不脱敏，可空；未绑定手机号时为 null，不补空串")
        String phone,
        @Schema(description = "是否已绑定手机号（portal_user.phone 非空）")
        boolean phoneBound,
        @Schema(description = "当前 AppID 下是否已绑定微信身份")
        boolean wechatBound,
        @Schema(description = "是否完成账号打通：手机号已绑、密码已设、微信已绑三者同时成立才为 true")
        boolean accountLinked) {
}
