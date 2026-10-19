package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.service.admin.AdminMerchantService;
import com.suzhong.njl.service.admin.AdminStoreService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminMerchantResponse;
import com.suzhong.njl.web.admin.dto.AdminStoreCreateRequest;
import com.suzhong.njl.web.admin.dto.AdminStoreResponse;
import com.suzhong.njl.web.admin.dto.MerchantStatusPatchRequest;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.ResetPasswordResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 运营端商家主体管理接口（{@code /api/admin/merchants}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：商家主体列表、启停切换、重置登录密码与代建门店。停用主体或账号会立即作废
 * 该主体的内存令牌；重置密码同样作废旧令牌，明文只在当次响应返回。
 */
@Tag(name = "运营 · 商家", description = "商家主体启停、重置密码与代建门店")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/merchants")
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;
    private final AdminStoreService adminStoreService;

    public AdminMerchantController(AdminMerchantService adminMerchantService, AdminStoreService adminStoreService) {
        this.adminMerchantService = adminMerchantService;
        this.adminStoreService = adminStoreService;
    }

    /**
     * {@code GET /api/admin/merchants} — 商家主体列表。
     *
     * <p>按主体 ID 倒序，每个主体带其全部登录账号（手机号、展示名、启用状态）。
     *
     * @param status   主体状态，取值 {@code ACTIVE} 或 {@code DISABLED}，为空不筛选
     * @param page     页码，从 1 开始，为空取 1
     * @param pageSize 每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页主体与总数
     */
    @Operation(summary = "商家主体列表", description = "按主体 ID 倒序，每个主体带其全部登录账号与启用状态。")
    @GetMapping
    public ApiResponse<PageResult<AdminMerchantResponse>> list(
            @RequestParam(required = false) MerchantOrgStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminMerchantService.list(status, page, pageSize));
    }

    /**
     * {@code PATCH /api/admin/merchants/{orgId}} — 修改主体状态或账号启用状态。
     *
     * <p>两个字段至少传一个：{@code status} 改主体，{@code enabled} 批量改该主体下所有账号。
     * 只要本次操作导致停用（主体 {@code DISABLED} 或 {@code enabled=false}），
     * 立即作废该主体的全部内存令牌。
     *
     * @param authentication 当前运营身份，用于审计
     * @param orgId          商家主体 ID
     * @param request        主体状态与账号启用状态，至少一项非空
     * @return 更新后的主体及其账号列表
     * @throws com.suzhong.njl.web.ApiException 两项都未传 400「请指定启停状态」；主体不存在 404
     */
    @Operation(summary = "修改主体状态", description = "status 与 enabled 至少传一项；停用立即作废该主体全部令牌，两项都空返回 400。")
    @PatchMapping("/{orgId}")
    public ApiResponse<AdminMerchantResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long orgId,
            @Valid @RequestBody MerchantStatusPatchRequest request) {
        return ApiResponse.ok(adminMerchantService.updateStatus(AdminPrincipal.adminId(authentication), orgId, request));
    }

    /**
     * {@code POST /api/admin/merchants/{orgId}/reset-password} — 重置主体登录密码。
     *
     * <p>请求体可省略；不传密码则随机生成，指定时长度 8 到 64 位。该主体下所有账号共用同一新密码，
     * 重置后立即作废该主体的全部内存令牌。明文只在本次响应返回。
     *
     * @param orgId   商家主体 ID
     * @param request 可选，仅含 {@code password}
     * @return 主体 ID、主账号 ID、手机号与一次性明文密码
     * @throws com.suzhong.njl.web.ApiException 主体不存在 404「商家不存在」；主体下无账号 404「商家账号不存在」；
     *                                         密码长度不合法 400
     */
    @Operation(summary = "重置商家密码", description = "请求体可省略，不传则随机生成；重置后作废全部令牌，明文只返回一次。")
    @PostMapping("/{orgId}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(
            @PathVariable Long orgId,
            @Valid @RequestBody(required = false) PlainPasswordRequest request) {
        return ApiResponse.ok(adminMerchantService.resetPassword(orgId, request));
    }

    /**
     * {@code POST /api/admin/merchants/{orgId}/stores} — 代建门店并绑定主体。
     *
     * <p>门店名称必填（不超过 128 字）、地区必填且必须启用；{@code shelfStatus} 缺省为 {@code DRAFT}，
     * {@code homeRecommended} 缺省 false，{@code homeSort} 缺省 100。相册只收 URL 数组，
     * 服务层序列化为 JSON 存储。创建后失效该门店详情缓存。
     *
     * @param orgId   商家主体 ID
     * @param request 门店字段与运营可控的上下架/推荐配置
     * @return 新建门店详情（含版本号 0）
     * @throws com.suzhong.njl.web.ApiException 商家不存在 404「商家不存在」；地区不可用或字段校验失败 400
     */
    @Operation(summary = "代建门店", description = "名称与地区必填，shelfStatus 缺省 DRAFT；相册只收 URL 数组，创建后失效门店缓存。")
    @PostMapping("/{orgId}/stores")
    public ApiResponse<AdminStoreResponse> createStore(
            @PathVariable Long orgId,
            @Valid @RequestBody AdminStoreCreateRequest request) {
        return ApiResponse.ok(adminStoreService.create(orgId, request));
    }
}
