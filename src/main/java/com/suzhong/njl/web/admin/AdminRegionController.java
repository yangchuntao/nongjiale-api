package com.suzhong.njl.web.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.service.admin.AdminCache;
import com.suzhong.njl.service.admin.RegionImportService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.RegionImportResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 运营端行政区划接口（{@code /api/admin/regions}）。
 *
 * <p>所属端：运营后台。需 Bearer 令牌，角色 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：把内置的全国省市区数据同步进库，并查询同步后的区划树。
 * 同步只新增和补全区划属性，不改开城状态与启停，因此可以反复执行。
 */
@Tag(name = "运营 · 行政区划", description = "全国省市区数据同步与查询")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/regions")
public class AdminRegionController {

    private final RegionImportService regionImportService;
    private final AdminCache adminCache;

    public AdminRegionController(RegionImportService regionImportService, AdminCache adminCache) {
        this.regionImportService = regionImportService;
        this.adminCache = adminCache;
    }

    /**
     * {@code POST /api/admin/regions/sync} — 同步内置区划数据。
     *
     * <p>把 jar 内的全国省市区数据导入 {@code region} 表。已存在的行政区划按 {@code adcode} 命中后只补名称、
     * 排序与经纬度；开城状态、启停、上级关系保持库内原值。重复执行结果一致。
     *
     * @return 新增、更新、跳过数量统计
     */
    @Operation(summary = "同步全国区划", description = "导入内置省市区数据；按 adcode 幂等，不覆盖开城状态与启停。")
    @PostMapping("/sync")
    public ApiResponse<RegionImportResult> sync() {
        RegionImportResult result = regionImportService.syncFromResource();
        // 同步可能新增了可见地区，门户地区树缓存要失效
        adminCache.evictCityOpen();
        return ApiResponse.ok(result);
    }

    /**
     * {@code GET /api/admin/regions} — 查询区划树。
     *
     * <p>按父级分组返回，键为父级 adcode，值为其下一级地区（按排序号升序）。
     * 省级的父级 adcode 为空字符串键。用于运营端按省浏览其下所有市，判断哪些还没开通。
     *
     * @return 父级 adcode 到子级列表的映射
     */
    @Operation(summary = "区划树", description = "按父级 adcode 分组返回下一级地区，按排序号升序。")
    @GetMapping
    public ApiResponse<Map<String, List<RegionBrief>>> tree() {
        Map<String, List<Region>> grouped = regionImportService.childrenByParent();
        Map<String, List<RegionBrief>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<Region>> entry : grouped.entrySet()) {
            List<RegionBrief> items = new ArrayList<>(entry.getValue().size());
            for (Region region : entry.getValue()) {
                items.add(new RegionBrief(
                        region.getId(),
                        region.getAdcode(),
                        region.getName(),
                        region.getLevel() == null ? null : region.getLevel().code(),
                        region.getOpenStatus() == null ? null : region.getOpenStatus().code(),
                        Boolean.TRUE.equals(region.getEnabled())));
            }
            result.put(entry.getKey(), items);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 区划节点摘要，用于运营端按省浏览。
     *
     * @param id         地区 ID
     * @param adcode     行政区划代码
     * @param name       地区全称
     * @param level      层级数字码：1 省、2 市、3 区县
     * @param openStatus 开通态码，仅市有值，其他层级为 null
     * @param enabled    是否启用
     */
    public record RegionBrief(
            Long id,
            String adcode,
            String name,
            Integer level,
            String openStatus,
            boolean enabled) {
    }
}
