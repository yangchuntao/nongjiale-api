package com.suzhong.njl.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 开城清单的五项勾选状态（{@code GET /api/admin/cities/{cityId}} 的 checklist 字段，
 * 清单修改、开通、关城接口的响应也复用本结构）。
 *
 * <p>①④⑤是运营手勾、落库在 {@code region_open_checklist}；②③不落库，
 * 每次按实时计数与阈值推导。新建城市没有清单行时，①④⑤一律为 false。
 *
 * @param destinationReady ①目的地数据是否齐备，手勾
 * @param supplyReady      ②供给是否达标，按 {@code storeCount >= minStores} 实时推导
 * @param articlesReady    ③攻略是否达标，按 {@code articleCount >= minArticles} 实时推导
 * @param supportCovered   ④客服覆盖是否已说明，手勾
 * @param dirtyDataCleared ⑤测数或脏数据是否已确认清理，手勾
 */
@Schema(name = "CityChecklistView", description = "开城清单五项勾选状态响应，用于 GET /api/admin/cities/{cityId} 的 checklist 字段，清单修改、开通、关城接口复用")
public record CityChecklistView(
        @Schema(description = "①目的地数据是否齐备，运营手勾")
        boolean destinationReady,
        @Schema(description = "②供给是否达标，按 storeCount >= minStores 实时推导，不落库")
        boolean supplyReady,
        @Schema(description = "③攻略是否达标，按 articleCount >= minArticles 实时推导，不落库")
        boolean articlesReady,
        @Schema(description = "④客服覆盖是否已说明，运营手勾")
        boolean supportCovered,
        @Schema(description = "⑤测数或脏数据是否已确认清理，运营手勾")
        boolean dirtyDataCleared) {
}
