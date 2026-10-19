package com.suzhong.njl.web.admin.dto;

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
public record AdminMerchantUserItem(
        Long id,
        String phone,
        String displayName,
        Boolean enabled) {
}
