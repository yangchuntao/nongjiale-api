package com.suzhong.njl.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家登录账号（{@code GET /api/admin/merchants} 列表项的 users 元素，
 * 也随 {@code PATCH /api/admin/merchants/{orgId}} 的响应返回）。
 *
 * <p>只暴露账号标识与启停状态，不返回密码哈希等敏感字段；手机号为登录号，不脱敏。
 *
 * @param id          账号 ID，对应 {@code merchant_user.id}
 * @param phone       登录手机号
 * @param displayName 账号展示名，审批通过时取联系人姓名，缺省用主体名称
 * @param enabled     账号是否启用；为 false 或主体为 DISABLED 时登录返回 403
 */
@Schema(name = "AdminMerchantUserItem", description = "商家登录账号响应，用于商家列表项的 users 元素，也随主体启停接口返回")
public record AdminMerchantUserItem(
        @Schema(description = "账号 ID，对应 merchant_user.id")
        Long id,
        @Schema(description = "登录手机号，运营端不脱敏")
        String phone,
        @Schema(description = "账号展示名，审批通过时取联系人姓名，缺省用主体名称")
        String displayName,
        @Schema(description = "账号是否启用；为 false 或主体为 DISABLED 时登录返回 403")
        Boolean enabled) {
}
