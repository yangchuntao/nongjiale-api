package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.ReviewStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家入驻申请提交结果（{@code POST /api/merchant-applies} 的响应体）。
 *
 * <p>只回传新记录 ID 与状态，不回传提交内容；状态在提交时固定为 {@code PENDING}。
 *
 * @param id     入驻申请记录 ID，供运营端定位
 * @param status 审核状态，见 {@link com.suzhong.njl.domain.enums.ReviewStatus}；新提交固定 {@code PENDING}
 */
@Schema(name = "MerchantApplyResponse", description = "商家入驻申请提交结果，POST /api/merchant-applies 的响应体；只回传 ID 与状态，不回传提交内容")
public record MerchantApplyResponse(
        @Schema(description = "入驻申请记录 ID，供运营端定位")
        Long id,
        @Schema(description = "审核状态：PENDING/APPROVED/REJECTED；新提交固定 PENDING")
        ReviewStatus status) {
}