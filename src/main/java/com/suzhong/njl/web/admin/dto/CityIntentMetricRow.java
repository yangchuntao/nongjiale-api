package com.suzhong.njl.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单行城市意向汇总（{@code GET /api/admin/metrics/city-intents} 的 rows 元素）。
 *
 * <p>口径：意向数统计时间窗内创建的全部意向（含已改期），已确认只数当前仍为 CONFIRMED 的记录，
 * 已入住只数当前为 CHECKED_IN 的记录；两者是当前状态快照，不是历史流转次数。
 * 意向通过门店所属地区归到市，门店已删除或地区无法归市的不计入任何行。
 *
 * @param cityId         市 ID；合计行为 null
 * @param cityName       市名称；合计行为「合计」
 * @param intentCount    意向数，含所有状态
 * @param confirmedCount 当前状态为 CONFIRMED 的数量
 * @param checkedInCount 当前状态为 CHECKED_IN 的数量，文案为「已入住」
 * @param total          是否为合计行；true 时 {@code cityId} 为 null
 */
@Schema(name = "CityIntentMetricRow", description = "单行城市意向汇总响应，用于 GET /api/admin/metrics/city-intents 的 rows 元素")
public record CityIntentMetricRow(
        @Schema(description = "市 ID；合计行为 null")
        Long cityId,
        @Schema(description = "市名称；合计行为「合计」")
        String cityName,
        @Schema(description = "意向数，统计时间窗内创建的全部意向，含已改期与所有状态")
        long intentCount,
        @Schema(description = "当前状态为 CONFIRMED 的数量，是状态快照而非历史流转次数")
        long confirmedCount,
        @Schema(description = "当前状态为 CHECKED_IN 的数量，文案为「已入住」")
        long checkedInCount,
        @Schema(description = "是否为合计行；true 时 cityId 为 null")
        boolean total) {
}
