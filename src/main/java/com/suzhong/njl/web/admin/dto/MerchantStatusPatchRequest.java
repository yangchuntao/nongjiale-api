package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(name = "MerchantStatusPatchRequest", description = "商家主体启停请求体，用于 PATCH /api/admin/merchants/{orgId}；status 与 enabled 至少传一个，都为空返回 400")
public record MerchantStatusPatchRequest(
        @Schema(description = "主体状态，可空，取值 ACTIVE（启用）、DISABLED（停用）；改主体状态，指向停用时吊销既有令牌")
        MerchantOrgStatus status,
        @Schema(description = "账号开关，可空；true 启用主体下全部账号，false 停用全部账号，停用后旧令牌立即失效")
        Boolean enabled) {
}
