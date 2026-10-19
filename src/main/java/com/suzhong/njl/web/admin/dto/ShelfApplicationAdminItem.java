package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.domain.enums.ShelfAction;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 上下架申请列表项（{@code GET /api/admin/shelf-applications} 的 items 元素，
 * 也是通过/驳回接口 data 的结构）。
 *
 * <p>申请通过后才改写门店的 {@code shelfStatus}：PUBLISH 转 ON_SHELF，UNPUBLISH 转 OFF_SHELF；
 * 驳回只改申请状态，门店可见性不变。同一门店同时只允许一条待审申请。
 *
 * @param id            申请 ID，对应 {@code shelf_application.id}
 * @param storeId       门店 ID
 * @param storeName     门店名称；门店已删除时为 null
 * @param merchantOrgId 门店所属商家主体 ID
 * @param action        申请动作，取值见 {@link ShelfAction}：PUBLISH（申请上架）、UNPUBLISH（申请下架）
 * @param reason        商家填写的申请说明，可空
 * @param status        审核状态，取值见 {@link ReviewStatus}：PENDING、APPROVED、REJECTED
 * @param rejectReason  驳回原因，仅已驳回时非空
 * @param reviewedBy    审核人运营 ID，未审核时为 null
 * @param reviewedAt    审核时间，未审核时为 null；{@code Asia/Shanghai} 的 ISO-8601
 * @param createdAt     申请创建时间，{@code Asia/Shanghai} 的 ISO-8601
 */
@Schema(name = "ShelfApplicationAdminItem", description = "上下架申请列表项响应，用于 GET /api/admin/shelf-applications 的 items 元素，也是通过/驳回接口 data 的结构")
public record ShelfApplicationAdminItem(
        @Schema(description = "申请 ID，对应 shelf_application.id")
        Long id,
        @Schema(description = "门店 ID")
        Long storeId,
        @Schema(description = "门店名称；门店已删除时为 null")
        String storeName,
        @Schema(description = "门店所属商家主体 ID")
        Long merchantOrgId,
        @Schema(description = "申请动作，取值 PUBLISH（申请上架）、UNPUBLISH（申请下架）；通过后 PUBLISH 转 ON_SHELF，UNPUBLISH 转 OFF_SHELF")
        ShelfAction action,
        @Schema(description = "商家填写的申请说明，可空")
        String reason,
        @Schema(description = "审核状态，取值 PENDING、APPROVED、REJECTED")
        ReviewStatus status,
        @Schema(description = "驳回原因，仅已驳回时非空")
        String rejectReason,
        @Schema(description = "审核人运营 ID，未审核时为 null")
        Long reviewedBy,
        @Schema(description = "审核时间，未审核时为 null，Asia/Shanghai 的 ISO-8601")
        LocalDateTime reviewedAt,
        @Schema(description = "申请创建时间，Asia/Shanghai 的 ISO-8601")
        LocalDateTime createdAt) {
}
