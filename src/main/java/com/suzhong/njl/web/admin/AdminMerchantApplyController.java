package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.service.admin.AdminMerchantApplyService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ApproveMerchantApplyResponse;
import com.suzhong.njl.web.admin.dto.MerchantApplyItem;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.RejectMerchantApplyResponse;
import com.suzhong.njl.web.admin.dto.RejectReasonRequest;
import com.suzhong.njl.web.portal.dto.MerchantApplyRequest;
import com.suzhong.njl.web.portal.dto.MerchantApplyResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 运营端商家入驻审核接口（{@code /api/admin/merchant-applies}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：入驻申请队列、通过并开通商家主体与主账号、驳回并记录原因。只有待审申请可处理，
 * 通过时明文密码只在当次响应返回一次，不落库、不回显。
 */
@Tag(name = "运营 · 入驻审核", description = "商家入驻申请队列，通过开通账号或驳回")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/merchant-applies")
public class AdminMerchantApplyController {

    private final AdminMerchantApplyService adminMerchantApplyService;

    public AdminMerchantApplyController(AdminMerchantApplyService adminMerchantApplyService) {
        this.adminMerchantApplyService = adminMerchantApplyService;
    }

    /**
     * {@code GET /api/admin/merchant-applies} — 入驻申请队列。
     *
     * <p>按创建时间倒序、ID 倒序；不传状态时返回全部。
     *
     * @param status   审核状态，取值 {@code PENDING}/{@code APPROVED}/{@code REJECTED}，为空不筛选
     * @param page     页码，从 1 开始，为空取 1
     * @param pageSize 每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页申请与总数
     */
    @Operation(summary = "入驻申请列表", description = "按创建时间倒序；status 为空时返回全部申请。")
    @GetMapping
    public ApiResponse<PageResult<MerchantApplyItem>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminMerchantApplyService.list(status, page, pageSize));
    }

    /**
     * {@code POST /api/admin/merchant-applies} — 运营代商家录入入驻申请。
     *
     * <p>复用门户提交的请求体与校验（主体名称必填、地区存在且启用、电话格式），
     * 落一条 {@code PENDING} 申请并在来源上标记 {@code ADMIN}，后续与商家自填申请
     * 走同一条审核流。允许重复提交。
     *
     * @param request 入驻资料：主体名（必填 ≤128 字）、联系人、电话、拟开门店名、地区、简介
     * @return 新申请 ID 与状态（固定 PENDING）
     * @throws com.suzhong.njl.web.ApiException 主体名称为空 400；地区不存在 400；电话格式不对 400
     */
    @Operation(summary = "代商家录入入驻申请", description = "校验规则与门户提交一致；落 PENDING 申请并标记来源 ADMIN。")
    @PostMapping
    public ApiResponse<MerchantApplyResponse> create(@Valid @RequestBody MerchantApplyRequest request) {
        return ApiResponse.ok(adminMerchantApplyService.create(request));
    }

    /**
     * {@code POST /api/admin/merchant-applies/{id}/approve} — 通过入驻申请并开通账号。
     *
     * <p>申请的联系电话必须是 11 位手机号，且不能已存在商家账号；通过时新建 {@code ACTIVE} 商家主体
     * 与一个启用的主账号。请求体可省略：不传密码则随机生成，密码长度 8 到 64 位。
     * 明文密码只在本次响应返回，服务端只存 BCrypt 哈希。
     *
     * @param authentication 当前运营身份，写入审核人
     * @param id             入驻申请 ID
     * @param request        可选，仅含 {@code password}
     * @return 申请 ID、审核状态、新主体/账号 ID、手机号与一次性明文密码
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404；已审核 409；电话无效 400；手机号已是商家账号 409
     */
    @Operation(summary = "通过入驻申请", description = "仅待审可处理，电话须为未注册的 11 位手机号；明文密码只返回一次，已审核 409。")
    @PostMapping("/{id}/approve")
    public ApiResponse<ApproveMerchantApplyResponse> approve(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PlainPasswordRequest request) {
        return ApiResponse.ok(adminMerchantApplyService.approve(AdminPrincipal.adminId(authentication), id, request));
    }

    /**
     * {@code POST /api/admin/merchant-applies/{id}/reject} — 驳回入驻申请。
     *
     * <p>驳回原因必填且不超过 500 字，去首尾空白后落库；驳回后不创建主体或账号，
     * 申请进入终态，不可再次审核。
     *
     * @param authentication 当前运营身份，写入审核人
     * @param id             入驻申请 ID
     * @param request        驳回原因，必填
     * @return 申请 ID、驳回状态与原因
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404；已审核 409；原因为空或超长 400
     */
    @Operation(summary = "驳回入驻申请", description = "仅待审可驳回；原因必填且不超过 500 字，驳回后进入终态不可再审核。")
    @PostMapping("/{id}/reject")
    public ApiResponse<RejectMerchantApplyResponse> reject(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectReasonRequest request) {
        return ApiResponse.ok(adminMerchantApplyService.reject(AdminPrincipal.adminId(authentication), id, request));
    }
}
