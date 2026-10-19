package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;

@JsonIgnoreProperties(ignoreUnknown = false)
/**
 * 商家主体启停请求体（{@code PATCH /api/admin/merchants/{orgId}}）。
 *
 * <p>{@code status} 与 {@code enabled} 至少传一个，都为空返回 400「请指定启停状态」。
 * {@code status} 改主体状态；{@code enabled} 批量改该主体下全部账号。
 * 任一操作指向停用都会吊销该主体既有令牌，停用后旧令牌立即失效。
 *
 * @param status  主体状态，可空；取值见 {@link MerchantOrgStatus}：ACTIVE（启用）、DISABLED（停用）
 * @param enabled 账号开关，可空；true 启用全部账号，false 停用全部账号
 */
public record MerchantStatusPatchRequest(
        MerchantOrgStatus status,
        Boolean enabled) {
}
