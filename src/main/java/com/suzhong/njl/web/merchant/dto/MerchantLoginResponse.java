package com.suzhong.njl.web.merchant.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家登录结果（{@code POST /api/merchant/auth/login} 的响应体）。
 *
 * <p>令牌角色固定 {@code MERCHANT}，只能访问 {@code /api/merchant/**}，且服务层会按
 * {@code merchantOrgId} 限定本主体数据。令牌存在进程内存，不进 Redis、不落库。
 *
 * @param token          访问令牌，放入 {@code Authorization: Bearer <token>}
 * @param expiresAt      令牌过期时间，UTC 瞬时点（ISO-8601）
 * @param merchantUserId 商家账号 ID，即 {@code merchant_user.id}
 * @param merchantOrgId  所属商家主体 ID，即 {@code merchant_org.id}
 * @param role           令牌角色，固定 {@link com.suzhong.njl.domain.enums.UserType#MERCHANT}
 * @param displayName    账号展示名，可空
 */
@Schema(name = "MerchantLoginResponse", description = "商家登录结果；令牌角色固定 MERCHANT，存在进程内存，不进 Redis、不落库")
public record MerchantLoginResponse(
        @Schema(description = "访问令牌，32 字节随机串（非 JWT），默认 12 小时有效；放入 Authorization: Bearer <token>")
        String token,
        @Schema(description = "令牌过期时间，UTC 瞬时点（ISO-8601）")
        Instant expiresAt,
        @Schema(description = "商家账号 ID，即 merchant_user.id")
        Long merchantUserId,
        @Schema(description = "所属商家主体 ID，即 merchant_org.id")
        Long merchantOrgId,
        @Schema(description = "令牌角色，固定 MERCHANT")
        UserType role,
        @Schema(description = "账号展示名，可空")
        String displayName) {
}
