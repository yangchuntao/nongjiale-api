package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;

/**
 * 商家主体（{@code GET /api/admin/merchants} 的 items 元素，以及
 * {@code PATCH /api/admin/merchants/{orgId}} 的 data）。
 *
 * <p>列表按主体 ID 倒序返回；{@code users} 是该主体下全部登录账号，按账号 ID 升序，
 * 停用主体时运营可据此确认哪些账号被一并关闭。
 *
 * @param id           主体 ID，对应 {@code merchant_org.id}
 * @param name         主体名称（入驻时的机构名）
 * @param contactName  联系人，可空
 * @param contactPhone 联系电话，可空；审批通过时用作商家登录手机号
 * @param status       主体状态，取值见 {@link MerchantOrgStatus}：ACTIVE（启用）、DISABLED（停用）
 * @param createdAt    创建时间，{@code Asia/Shanghai} 的 ISO-8601
 * @param users        登录账号列表，没有账号时为空列表；账号的 {@code enabled} 与主体状态共同决定能否登录
 */
public record AdminMerchantResponse(
        Long id,
        String name,
        String contactName,
        String contactPhone,
        MerchantOrgStatus status,
        LocalDateTime createdAt,
        List<AdminMerchantUserItem> users) {
}
