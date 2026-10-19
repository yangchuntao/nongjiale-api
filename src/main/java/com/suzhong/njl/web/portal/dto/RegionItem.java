package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.SeedMarket;

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
public record RegionItem(
        Long id,
        Long parentId,
        String slug,
        String name,
        RegionLevel level,
        Integer sortOrder,
        SeedMarket seedMarket) {
}
