package com.suzhong.njl.web.admin;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.service.admin.AdminBannerService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.BannerAdminResponse;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 运营端首页 Banner 管理接口（{@code /api/admin/banners}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：Banner 的增删改查与行内启停。投放范围分 {@code ALL}（全站）与 {@code CITY}（指定市），
 * 指定市必须存在且处于开通状态；图片与跳转链接只收 URL/站内路径，不做文件上传。
 */
@Tag(name = "运营 · 首页 Banner", description = "首页 Banner 的增删改查与启停，图片与链接只收 URL")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    public AdminBannerController(AdminBannerService adminBannerService) {
        this.adminBannerService = adminBannerService;
    }

    /**
     * {@code GET /api/admin/banners} — 分页查询 Banner。
     *
     * <p>按 {@code sortOrder} 升序、ID 升序；关键词同时匹配标题与跳转链接。
     *
     * @param enabled  启用状态筛选，为空不筛选
     * @param scope    投放范围筛选，取值 {@code ALL} 或 {@code CITY}，为空不筛选
     * @param keyword  标题/链接关键词，去空白后超过 64 字返回 400「参数不正确」，空白视同未传
     * @param page     页码，从 1 开始，为空取 1
     * @param pageSize 每页条数，1 到 100，为空取 20；越界返回 400
     * @return 当页 Banner（含市 slug、市名与投放市当前是否开通）与总数
     */
    @Operation(summary = "Banner 列表", description = "按排序号、ID 升序；关键词同时匹配标题与跳转链接，超 64 字返回 400。")
    @GetMapping
    public ApiResponse<PageResult<BannerAdminResponse>> list(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) BannerScope scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminBannerService.list(enabled, scope, keyword, page, pageSize));
    }

    /**
     * {@code POST /api/admin/banners} — 新建 Banner。
     *
     * <p>校验顺序：图片与链接 → 标题 → 排序 → 投放范围与城市 → 城市是否开通 → 启用状态。
     * 图片须为 http/https URL（不超过 512 字），链接须为 http/https 外链或以单个斜杠开头的站内路径；
     * 范围 {@code ALL} 不能带 {@code cityId}，{@code CITY} 必须带且该市须已开通。
     * 新建时 {@code enabled} 缺省为 true，{@code sortOrder} 缺省为 100。
     *
     * @param authentication 当前运营身份，用于审计与创建人
     * @param request        Banner 字段，未加 {@code @Valid}，全部由服务层校验
     * @return 新建后的 Banner
     * @throws com.suzhong.njl.web.ApiException 字段非法 400；城市不存在 404；投放市未开通 400
     */
    @Operation(summary = "新建 Banner", description = "ALL 不能带城市，CITY 须指定已开通城市；enabled 缺省 true，sortOrder 缺省 100。")
    @PostMapping
    public ApiResponse<BannerAdminResponse> create(
            Authentication authentication,
            @RequestBody BannerWriteRequest request) {
        return ApiResponse.ok(adminBannerService.create(AdminPrincipal.adminId(authentication), request));
    }

    /**
     * {@code GET /api/admin/banners/{id}} — Banner 详情。
     *
     * @param id Banner ID
     * @return Banner 完整字段，含投放市的 slug、名称与开通状态
     * @throws com.suzhong.njl.web.ApiException Banner 不存在 404「Banner 不存在」
     */
    @Operation(summary = "Banner 详情", description = "含投放市 slug、名称与开通状态；不存在返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<BannerAdminResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminBannerService.detail(id));
    }

    /**
     * {@code PUT /api/admin/banners/{id}} — 全量更新 Banner。
     *
     * <p>整单覆盖，必填项缺失直接 400。更新场景下 {@code enabled} 与 {@code sortOrder} 必填；
     * 投放范围、城市与开城校验同新建，投放市未开通时返回 400。
     *
     * @param authentication 当前运营身份，用于审计与更新人
     * @param id             Banner ID
     * @param request        Banner 全量字段
     * @return 更新后的 Banner
     * @throws com.suzhong.njl.web.ApiException 字段非法 400；Banner 或城市不存在 404；投放市未开通 400
     */
    @Operation(summary = "全量更新 Banner", description = "整单覆盖，enabled 与 sortOrder 必填；投放市未开通返回 400。")
    @PutMapping("/{id}")
    public ApiResponse<BannerAdminResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody BannerWriteRequest request) {
        return ApiResponse.ok(adminBannerService.update(AdminPrincipal.adminId(authentication), id, request));
    }

    /**
     * {@code POST /api/admin/banners/{id}/enable} — 启用 Banner。
     *
     * <p>已启用时幂等返回当前记录。按市投放的 Banner 在启用时额外校验投放市当前仍开通，
     * 已关城则拒绝，避免首页出现指向不可用目的地的广告。
     *
     * @param authentication 当前运营身份，用于审计
     * @param id             Banner ID
     * @return 启用后的 Banner
     * @throws com.suzhong.njl.web.ApiException Banner 不存在 404；投放市已关闭 409「投放市未开通」
     */
    @Operation(summary = "启用 Banner", description = "已启用时幂等返回；按市投放的 Banner 若投放市已关城返回 409。")
    @PostMapping("/{id}/enable")
    public ApiResponse<BannerAdminResponse> enable(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(adminBannerService.setEnabled(AdminPrincipal.adminId(authentication), id, true));
    }

    /**
     * {@code POST /api/admin/banners/{id}/disable} — 停用 Banner。
     *
     * <p>已停用时幂等返回当前记录；停用立即影响门户，不校验投放市状态。
     *
     * @param authentication 当前运营身份，用于审计
     * @param id             Banner ID
     * @return 停用后的 Banner
     * @throws com.suzhong.njl.web.ApiException Banner 不存在 404
     */
    @Operation(summary = "停用 Banner", description = "已停用时幂等返回；停用立即影响门户，不校验投放市状态。")
    @PostMapping("/{id}/disable")
    public ApiResponse<BannerAdminResponse> disable(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(adminBannerService.setEnabled(AdminPrincipal.adminId(authentication), id, false));
    }

    /**
     * {@code DELETE /api/admin/banners/{id}} — 物理删除 Banner。
     *
     * <p>不做软删除；删除成功后立即从门户首页消失。
     *
     * @param authentication 当前运营身份，用于审计
     * @param id             Banner ID
     * @return 固定结构 {@code {"id": BannerID}}
     * @throws com.suzhong.njl.web.ApiException Banner 不存在 404
     */
    @Operation(summary = "删除 Banner", description = "物理删除；删除后立即从门户首页消失。")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Long>> delete(Authentication authentication, @PathVariable Long id) {
        adminBannerService.delete(AdminPrincipal.adminId(authentication), id);
        return ApiResponse.ok(Map.of("id", id));
    }
}
