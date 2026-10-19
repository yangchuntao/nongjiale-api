package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.service.admin.AdminStoreService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminStoreResponse;
import com.suzhong.njl.web.admin.dto.AdminStoreUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 运营端门店管理接口（{@code /api/admin/stores}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：全站门店查询与代编。代编可改上下架、首页推荐与测试数据标记；直接改
 * {@code shelfStatus} 会立即生效并写 {@code SHELF_FORCE} 审计，不走商家申请队列。
 */
@Tag(name = "运营 · 门店", description = "全站门店查询与代编，可直接改上下架与推荐位")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    public AdminStoreController(AdminStoreService adminStoreService) {
        this.adminStoreService = adminStoreService;
    }

    /**
     * {@code GET /api/admin/stores} — 全站门店列表。
     *
     * <p>按门店 ID 倒序；门店名称为模糊匹配（去首尾空白后），其余条件等值过滤。
     * 与门户不同，这里包含草稿、下架与测试数据。
     *
     * @param shelfStatus     上下架状态，取值 {@code ON_SHELF}/{@code OFF_SHELF}/{@code DRAFT}
     * @param regionId        地区 ID，可选
     * @param merchantOrgId   商家主体 ID，可选
     * @param homeRecommended 是否首页推荐，可选
     * @param name            门店名关键词，空白视同未传
     * @param page            页码，从 1 开始，为空取 1
     * @param pageSize        每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页门店与总数
     */
    @Operation(summary = "门店列表", description = "门店名模糊匹配、其余条件等值过滤；含草稿、下架与测试数据。")
    @GetMapping
    public ApiResponse<PageResult<AdminStoreResponse>> list(
            @RequestParam(required = false) ShelfStatus shelfStatus,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) Boolean homeRecommended,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminStoreService.list(
                shelfStatus, regionId, merchantOrgId, homeRecommended, name, page, pageSize));
    }

    /**
     * {@code GET /api/admin/stores/{id}} — 门店详情。
     *
     * @param id 门店 ID
     * @return 门店完整字段，含商家主体、版本号与测试数据标记
     * @throws com.suzhong.njl.web.ApiException 门店不存在 404「门店不存在」
     */
    @Operation(summary = "门店详情", description = "返回门店完整字段，含商家主体、版本号与测试数据标记；不存在 404。")
    @GetMapping("/{id}")
    public ApiResponse<AdminStoreResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminStoreService.detail(id));
    }

    /**
     * {@code PUT /api/admin/stores/{id}} — 代编门店（整单保存）。
     *
     * <p>名称、地区、首页推荐、首页排序、上下架状态与版本号必填；地址、电话、封面等长度受限。
     * {@code shelfStatus} 与当前值不同即代上下架，立即生效并写 {@code SHELF_FORCE} 审计；
     * {@code version} 用于乐观锁，冲突返回 409。保存后失效门店详情缓存。
     *
     * @param authentication 当前运营身份，用于审计
     * @param id             门店 ID
     * @param request        门店全量字段，含运营专属的推荐位、上下架与测试数据标记
     * @return 更新后的门店详情
     * @throws com.suzhong.njl.web.ApiException 门店不存在 404；字段校验失败、地区不可用或图集格式错误 400；
     *                                         版本冲突 409「数据已被修改」
     */
    @Operation(summary = "代编门店", description = "整单保存；改上下架立即生效并写 SHELF_FORCE 审计，版本冲突返回 409。")
    @PutMapping("/{id}")
    public ApiResponse<AdminStoreResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminStoreUpdateRequest request) {
        return ApiResponse.ok(adminStoreService.update(AdminPrincipal.adminId(authentication), id, request));
    }
}
