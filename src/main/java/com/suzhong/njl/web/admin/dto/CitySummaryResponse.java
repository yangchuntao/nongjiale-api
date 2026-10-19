package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.RegionOpenStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 开城列表项（{@code GET /api/admin/cities} 的 data 数组元素）。
 *
 * <p>该接口不分页，只列 {@code enabled=true} 且层级为市的节点，按 {@code sortOrder}、ID 升序；
 * 供给与攻略数按实时数据统计：上架且非测试、地区启用的门店，以及已发布攻略。
 *
 * @param id           市 ID，对应 {@code region.id}
 * @param provinceName 父省名称，可空；父节点不是省或缺失时为 null
 * @param name         市名称，如扬州市
 * @param slug         市 slug，如 yangzhou，用于门户链接
 * @param openStatus   对客开通态，取值见 {@link RegionOpenStatus}：OPEN（已开通）、CLOSED（未开通）；库内空值按 CLOSED 返回
 * @param storeCount   上架、非测试且地区启用的门店数，含下属区县
 * @param articleCount 已发布且挂在该市或下属区县的攻略数
 * @param updatedAt    清单最近操作时间，可空；没有清单行时为 null
 */
@Schema(name = "CitySummaryResponse", description = "开城列表项响应，用于 GET /api/admin/cities 的 data 数组元素")
public record CitySummaryResponse(
        @Schema(description = "市 ID，对应 region.id")
        Long id,
        @Schema(description = "父省名称，可空；父节点不是省或缺失时为 null")
        String provinceName,
        @Schema(description = "市名称，如扬州市")
        String name,
        @Schema(description = "市 slug，如 yangzhou，用于门户链接")
        String slug,
        @Schema(description = "对客开通态，取值 OPEN（已开通）、CLOSED（未开通）；库内空值按 CLOSED 返回")
        RegionOpenStatus openStatus,
        @Schema(description = "上架、非测试且地区启用的门店数，含下属区县")
        long storeCount,
        @Schema(description = "已发布且挂在该市或下属区县的攻略数")
        long articleCount,
        @Schema(description = "清单最近操作时间，可空；没有清单行时为 null，格式为 Asia/Shanghai 的 ISO-8601")
        LocalDateTime updatedAt) {
}
