package com.suzhong.njl.web.merchant.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.domain.enums.ShelfAction;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家上下架申请（{@code POST /api/merchant/stores/{id}/shelf-applications} 的响应体，以及
 * {@code GET /api/merchant/shelf-applications} 的 items 元素）。
 *
 * <p>只返回本商家主体的申请，按创建时间倒序、ID 倒序。提交时状态固定 {@code PENDING}；
 * 审批通过后 {@code PUBLISH} 会把门店置为 {@code ON_SHELF}，{@code UNPUBLISH} 置为 {@code OFF_SHELF}；
 * 驳回只记录原因，门店可见性不变。
 *
 * @param id           申请 ID
 * @param storeId      门店 ID
 * @param storeName    门店展示名，可空；门店被删除后列表里取不到名字
 * @param merchantOrgId 所属商家主体 ID，即 {@code merchant_org.id}
 * @param action       申请动作，见 {@link com.suzhong.njl.domain.enums.ShelfAction}：{@code PUBLISH}/{@code UNPUBLISH}
 * @param reason       商家填写的申请原因，可空
 * @param status       审核状态，见 {@link com.suzhong.njl.domain.enums.ReviewStatus}：{@code PENDING}/{@code APPROVED}/{@code REJECTED}
 * @param rejectReason 驳回原因，可空；仅 {@code REJECTED} 时有值
 * @param createdAt    提交时间，服务器本地时间（Asia/Shanghai）
 */
@Schema(name = "ShelfApplicationResponse", description = "商家上下架申请；只返回本商家主体的申请，按创建时间倒序、ID 倒序")
public record ShelfApplicationResponse(
        @Schema(description = "申请 ID")
        Long id,
        @Schema(description = "门店 ID")
        Long storeId,
        @Schema(description = "门店展示名，可空；门店被删除后取不到名字")
        String storeName,
        @Schema(description = "所属商家主体 ID，即 merchant_org.id")
        Long merchantOrgId,
        @Schema(description = "申请动作：PUBLISH 申请上架、UNPUBLISH 申请下架", allowableValues = {"PUBLISH", "UNPUBLISH"})
        ShelfAction action,
        @Schema(description = "商家填写的申请原因，可空")
        String reason,
        @Schema(description = "审核状态：PENDING 待审、APPROVED 通过、REJECTED 驳回",
                allowableValues = {"PENDING", "APPROVED", "REJECTED"})
        ReviewStatus status,
        @Schema(description = "驳回原因，可空；仅 REJECTED 时有值")
        String rejectReason,
        @Schema(description = "提交时间，服务器本地时间（Asia/Shanghai）")
        LocalDateTime createdAt) {
}
