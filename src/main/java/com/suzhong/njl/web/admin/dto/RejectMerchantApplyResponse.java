package com.suzhong.njl.web.admin.dto;

import com.suzhong.njl.domain.enums.ReviewStatus;

/**
 * 入驻审核驳回响应（{@code POST /api/admin/merchant-applies/{id}/reject} 的 data）。
 *
 * <p>驳回后不创建主体与账号，原申请记录保留，状态进入终态；对非待审申请再次审核返回 409。
 *
 * @param id           入驻申请 ID
 * @param status       审核结果，固定为 {@link ReviewStatus#REJECTED}
 * @param rejectReason 驳回原因，取自请求体并去除首尾空白，最长 500 字
 */
public record RejectMerchantApplyResponse(
        Long id,
        ReviewStatus status,
        String rejectReason) {
}
