package com.suzhong.njl.web.admin.dto;

/**
 * 重置商家密码响应（{@code POST /api/admin/merchants/{orgId}/reset-password} 的 data）。
 *
 * <p>该主体下全部账号会被改成同一密码，并吊销主体既有令牌；
 * 响应只带 id 最小的主账号信息作为登录提示，明文密码只返回一次。
 *
 * @param merchantOrgId  商家主体 ID
 * @param merchantUserId 主体下 ID 最小的账号 ID，作为主账号提示
 * @param phone          主账号手机号
 * @param plainPassword  明文密码，只在本响应返回一次；未传密码时由服务端生成 10 位随机串
 */
public record ResetPasswordResponse(
        Long merchantOrgId,
        Long merchantUserId,
        String phone,
        String plainPassword) {
}
