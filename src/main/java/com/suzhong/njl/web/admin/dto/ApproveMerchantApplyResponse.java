package com.suzhong.njl.web.admin.dto;

import com.suzhong.njl.domain.enums.ReviewStatus;

/**
 * 入驻审核通过响应（{@code POST /api/admin/merchant-applies/{id}/approve} 的 data）。
 *
 * <p>通过时同步创建商家主体与首个登录账号；{@code plainPassword} 是唯一一次可见的明文，
 * 后端只存哈希，后续无法找回，运营需当场转交商家。
 *
 * @param id             入驻申请 ID
 * @param status         审核结果，固定为 {@link ReviewStatus#APPROVED}
 * @param merchantOrgId  新建的商家主体 ID
 * @param merchantUserId 新建的商家登录账号 ID
 * @param phone          申请联系电话，即商家登录手机号
 * @param plainPassword  明文密码，只在本响应返回一次；未传密码时由服务端生成 10 位随机串
 */
public record ApproveMerchantApplyResponse(
        Long id,
        ReviewStatus status,
        Long merchantOrgId,
        Long merchantUserId,
        String phone,
        String plainPassword) {
}
