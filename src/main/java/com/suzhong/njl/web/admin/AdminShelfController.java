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
import com.suzhong.njl.service.admin.AdminShelfService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.RejectReasonRequest;
import com.suzhong.njl.web.admin.dto.ShelfApplicationAdminItem;

import jakarta.validation.Valid;

/**
 * 运营端上下架审核接口（{@code /api/admin/shelf-applications}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：查看商家提交的上架/下架申请并审批。通过后立即改门店 {@code shelfStatus}
 * 并失效门店缓存；驳回只记录原因，不改变门店可见性。只有待审申请可处理。
 */
@RestController
@RequestMapping("/api/admin/shelf-applications")
public class AdminShelfController {

    private final AdminShelfService adminShelfService;

    public AdminShelfController(AdminShelfService adminShelfService) {
        this.adminShelfService = adminShelfService;
    }

    /**
     * {@code GET /api/admin/shelf-applications} — 上下架申请队列。
     *
     * <p>按创建时间倒序、ID 倒序；门店名按当前门店表补全，门店已删除时为空。
     *
     * @param status       审核状态，取值 {@code PENDING}/{@code APPROVED}/{@code REJECTED}，为空不筛选
     * @param storeId      门店 ID，可选
     * @param merchantOrgId 商家主体 ID，可选
     * @param page         页码，从 1 开始，为空取 1
     * @param pageSize     每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页申请与总数
     */
    @GetMapping
    public ApiResponse<PageResult<ShelfApplicationAdminItem>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminShelfService.list(status, storeId, merchantOrgId, page, pageSize));
    }

    /**
     * {@code POST /api/admin/shelf-applications/{id}/approve} — 通过上下架申请。
     *
     * <p>按申请动作落地：{@code PUBLISH} 置门店为 {@code ON_SHELF}，{@code UNPUBLISH} 置为
     * {@code OFF_SHELF}；同时写审核人与审核时间，记录审计并失效门店详情缓存。
     *
     * @param authentication 当前运营身份，写入审核人
     * @param id             申请 ID
     * @return 审核后的申请（含门店名、动作与状态）
     * @throws com.suzhong.njl.web.ApiException 申请不存在或门店不存在 404；申请已审核 409「申请已审核」；
     *                                         门店更新冲突 409「数据已被修改」
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<ShelfApplicationAdminItem> approve(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(adminShelfService.approve(AdminPrincipal.adminId(authentication), id));
    }

    /**
     * {@code POST /api/admin/shelf-applications/{id}/reject} — 驳回上下架申请。
     *
     * <p>驳回原因必填且不超过 500 字；门店上下架状态与门户可见性保持不变，
     * 商家可修正后重新提交。
     *
     * @param authentication 当前运营身份，写入审核人
     * @param id             申请 ID
     * @param request        驳回原因，必填
     * @return 审核后的申请
     * @throws com.suzhong.njl.web.ApiException 申请不存在 404；申请已审核 409；原因为空或超长 400
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<ShelfApplicationAdminItem> reject(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectReasonRequest request) {
        return ApiResponse.ok(adminShelfService.reject(AdminPrincipal.adminId(authentication), id, request));
    }
}
