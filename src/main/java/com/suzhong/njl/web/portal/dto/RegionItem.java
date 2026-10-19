package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.SeedMarket;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 门户地区项（{@code GET /api/regions} 的响应数组元素，公开接口）。
 *
 * <p>只包含已启用且对客可见的地区：省必须有至少一个已开通市，市必须自身已开通，
 * 区县跟随父市开通态；未开通市及其区县不出现。结果按 {@code sortOrder}、ID 升序。
 *
 * @param id         地区 ID
 * @param parentId   父地区 ID，可空；省为 null，市挂省，区县挂市
 * @param slug       地区英文标识，用于门户路由，例如 {@code yangzhou}
 * @param name       地区展示名
 * @param level      行政层级，见 {@link com.suzhong.njl.domain.enums.RegionLevel}：{@code PROVINCE}/{@code CITY}/{@code DISTRICT}
 * @param sortOrder  排序号，升序；可空
 * @param seedMarket 所属种子市场，见 {@link com.suzhong.njl.domain.enums.SeedMarket}；当前只有 {@code SUZHONG}，不参与可见性判断
 */
@Schema(name = "RegionItem", description = "门户地区项，GET /api/regions 的响应数组元素；只包含已启用且对客可见的地区，按 sortOrder、ID 升序")
public record RegionItem(
        @Schema(description = "地区 ID")
        Long id,
        @Schema(description = "父地区 ID，可空；省为 null，市挂省，区县挂市")
        Long parentId,
        @Schema(description = "地区英文标识，用于门户路由", example = "yangzhou")
        String slug,
        @Schema(description = "地区展示名", example = "扬州市")
        String name,
        @Schema(description = "行政层级：PROVINCE 省、CITY 市、DISTRICT 区县")
        RegionLevel level,
        @Schema(description = "排序号，升序；可空")
        Integer sortOrder,
        @Schema(description = "所属种子市场：当前只有 SUZHONG（苏中），不参与可见性判断")
        SeedMarket seedMarket) {
}
