package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(name = "AdminMerchantResponse", description = "商家主体响应，用于 GET /api/admin/merchants 的 items 元素及 PATCH /api/admin/merchants/{orgId} 的 data")
public record AdminMerchantResponse(
        @Schema(description = "主体 ID，对应 merchant_org.id")
        Long id,
        @Schema(description = "主体名称（入驻时的机构名）")
        String name,
        @Schema(description = "联系人，可空")
        String contactName,
        @Schema(description = "联系电话，可空，运营端不脱敏；审批通过时用作商家登录手机号")
        String contactPhone,
        @Schema(description = "主体状态，取值 ACTIVE（启用）、DISABLED（停用）")
        MerchantOrgStatus status,
        @Schema(description = "创建时间，Asia/Shanghai 的 ISO-8601")
        LocalDateTime createdAt,
        @Schema(description = "登录账号列表，没有账号时为空列表；账号 enabled 与主体状态共同决定能否登录")
        List<AdminMerchantUserItem> users) {
}
