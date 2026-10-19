package com.suzhong.njl.web.admin.dto;

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
public record CityChecklistView(
        boolean destinationReady,
        boolean supplyReady,
        boolean articlesReady,
        boolean supportCovered,
        boolean dirtyDataCleared) {
}
