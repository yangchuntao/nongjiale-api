package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ReviewStatus;

/**
 * 入驻申请列表项（{@code GET /api/admin/merchant-applies} 的 items 元素）。
 *
 * <p>列表按创建时间倒序、ID 倒序返回，可按 {@code status} 筛选；字段基本来自申请记录原样透出，
 * 不做脱敏，便于运营联系申请人。审核通过后 {@code merchantOrgId} 才有值。
 *
 * @param id               申请 ID，对应 {@code merchant_apply.id}
 * @param orgName          申请机构名称，非空
 * @param contactName      联系人姓名，可空
 * @param contactPhone     联系电话，可空；审批通过时用作商家登录手机号，必须满足手机号格式
 * @param plannedStoreName 计划门店名，可空；仅供运营参考，不会自动建店
 * @param regionId         意向地区 ID，可空，对应 {@code region.id}
 * @param description      商家介绍，可空，最长 1000 字
 * @param licenseUrls      资质图片 URL 文本，可空；门户提交接口不接收，多为历史数据或运营补录
 * @param status           审核状态，取值见 {@link ReviewStatus}：PENDING、APPROVED、REJECTED
 * @param rejectReason     驳回原因，仅已驳回时非空
 * @param reviewedBy       审核人运营 ID，未审核时为 null
 * @param reviewedAt       审核时间，未审核时为 null；{@code Asia/Shanghai} 的 ISO-8601
 * @param merchantOrgId    审核通过后新建的商家主体 ID，其他状态为 null
 * @param createdAt        申请创建时间，{@code Asia/Shanghai} 的 ISO-8601
 */
public record MerchantApplyItem(
        Long id,
        String orgName,
        String contactName,
        String contactPhone,
        String plannedStoreName,
        Long regionId,
        String description,
        String licenseUrls,
        ReviewStatus status,
        String rejectReason,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long merchantOrgId,
        LocalDateTime createdAt) {
}
