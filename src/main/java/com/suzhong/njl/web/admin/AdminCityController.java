package com.suzhong.njl.web.admin;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.admin.AdminCityService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.ChecklistPatchRequest;
import com.suzhong.njl.web.admin.dto.CityDetailResponse;
import com.suzhong.njl.web.admin.dto.CitySummaryResponse;
import com.suzhong.njl.web.admin.dto.CityVersionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 运营端开城管理接口（{@code /api/admin/cities}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：开城列表与详情、人工清单勾选、开通与关城。供给数与攻略数由服务层实时统计，
 * 不能手改；开通要求清单项齐备且供给/攻略达到配置门槛，关城不删除历史数据。
 */
@Tag(name = "运营 · 开城", description = "开城清单、达标校验与城市开通/关闭")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/cities")
public class AdminCityController {

    private final AdminCityService adminCityService;

    public AdminCityController(AdminCityService adminCityService) {
        this.adminCityService = adminCityService;
    }

    /**
     * {@code GET /api/admin/cities} — 开城列表。
     *
     * <p>只返回启用中的市级地区，按地区排序号、ID 升序；供给量与攻略量为实时统计，
     * 供给只计已上架且非测试数据的门店。
     *
     * @return 市级摘要列表，含开通状态、供给数、攻略数与清单更新时间
     */
    @Operation(summary = "开城列表", description = "只返回启用中的市级地区，供给只计已上架且非测试门店，数据实时统计。")
    @GetMapping
    public ApiResponse<List<CitySummaryResponse>> list() {
        return ApiResponse.ok(adminCityService.list());
    }

    /**
     * {@code GET /api/admin/cities/{cityId}} — 开城详情。
     *
     * <p>返回人工清单、实时供给/攻略达标情况、缺失原因、能否开通/关城以及乐观锁版本号。
     *
     * @param cityId 市级地区 ID
     * @return 城市详情；供给、攻略为实时计数，{@code supplyReady}/{@code articlesReady} 只读
     * @throws com.suzhong.njl.web.ApiException 地区不存在或不是市级 404「城市不存在」；地区已停用 400「地区不可用」
     */
    @Operation(summary = "开城详情", description = "返回人工清单、实时达标情况与乐观锁版本号；不是市级地区返回 404。")
    @GetMapping("/{cityId}")
    public ApiResponse<CityDetailResponse> detail(@PathVariable Long cityId) {
        return ApiResponse.ok(adminCityService.detail(cityId));
    }

    /**
     * {@code PATCH /api/admin/cities/{cityId}/checklist} — 修改人工开城清单。
     *
     * <p>只允许改 {@code destinationReady}、{@code supportCovered}、{@code dirtyDataCleared} 三项，
     * 且至少要传一项；{@code supplyReady}/{@code articlesReady} 为实时统计，出现即 400，
     * 避免运营误以为手改已保存。{@code version} 必填，用于乐观锁。
     *
     * @param authentication 当前运营身份，用于审计与更新时间
     * @param cityId         市级地区 ID
     * @param request        清单项与版本号；未传的清单项保持原值
     * @return 更新后的城市详情
     * @throws com.suzhong.njl.web.ApiException 带自动统计字段、未指定更新项、地区不可用或版本为空 400；
     *                                         城市不存在 404；版本冲突 409「数据已被修改」
     */
    @Operation(summary = "修改开城清单", description = "只允许改三项人工清单且至少传一项；自动统计字段出现即 400，版本冲突 409。")
    @PatchMapping("/{cityId}/checklist")
    public ApiResponse<CityDetailResponse> patch(
            Authentication authentication,
            @PathVariable Long cityId,
            @Valid @RequestBody ChecklistPatchRequest request) {
        return ApiResponse.ok(adminCityService.patch(AdminPrincipal.adminId(authentication), cityId, request));
    }

    /**
     * {@code POST /api/admin/cities/{cityId}/open} — 开通城市。
     *
     * <p>要求人工清单三项均为 true，且已上架非测试门店数、已发布攻略数达到配置门槛；
     * 任一不满足都不改状态并返回 409，响应消息列出缺失原因。成功后失效城市可见性相关缓存。
     *
     * @param authentication 当前运营身份，用于审计
     * @param cityId         市级地区 ID
     * @param request        只含乐观锁版本号，必填
     * @return 开通后的城市详情
     * @throws com.suzhong.njl.web.ApiException 城市不存在 404；地区不可用或版本为空 400；
     *                                         已开通、未达标或版本冲突 409
     */
    @Operation(summary = "开通城市", description = "清单三项齐备且供给/攻略达门槛才可开通，未达标或版本冲突返回 409。")
    @PostMapping("/{cityId}/open")
    public ApiResponse<CityDetailResponse> open(
            Authentication authentication,
            @PathVariable Long cityId,
            @Valid @RequestBody CityVersionRequest request) {
        return ApiResponse.ok(adminCityService.open(AdminPrincipal.adminId(authentication), cityId, request.version()));
    }

    /**
     * {@code POST /api/admin/cities/{cityId}/close} — 关闭城市。
     *
     * <p>只有已开通的市可关闭；关城只改开通状态，不删除门店、攻略或历史意向，
     * 但门户列表、首页推荐与 Banner 会立即按未开通处理。
     *
     * @param authentication 当前运营身份，用于审计
     * @param cityId         市级地区 ID
     * @param request        只含乐观锁版本号，必填
     * @return 关城后的城市详情
     * @throws com.suzhong.njl.web.ApiException 城市不存在 404；地区不可用或版本为空 400；
     *                                         该市未开通或版本冲突 409
     */
    @Operation(summary = "关闭城市", description = "仅已开通的市可关闭；关城不删除历史数据，未开通或版本冲突返回 409。")
    @PostMapping("/{cityId}/close")
    public ApiResponse<CityDetailResponse> close(
            Authentication authentication,
            @PathVariable Long cityId,
            @Valid @RequestBody CityVersionRequest request) {
        return ApiResponse.ok(adminCityService.close(AdminPrincipal.adminId(authentication), cityId, request.version()));
    }
}
