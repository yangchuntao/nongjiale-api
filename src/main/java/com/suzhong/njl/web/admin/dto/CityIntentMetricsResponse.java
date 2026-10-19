package com.suzhong.njl.web.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 城市意向汇总（{@code GET /api/admin/metrics/city-intents} 的 data）。
 *
 * <p>时间打在 {@code booking_intent.created_at}；{@code 7} 与 {@code 30} 是从当前时刻往前滚动的天数，
 * 不是自然日零点。{@code cityId} 过滤时只返回该市一行，合计行仍然存在。
 *
 * @param window 实际生效的时间窗，取值 7、30、ALL；省略参数时默认 7
 * @param from   统计下界，带时区偏移的 ISO-8601（{@code Asia/Shanghai}，如 {@code 2026-10-12T10:00:00+08:00}）；ALL 时为 null
 * @param rows   第一行固定为合计（{@code total=true}），其后按市排序
 */
public record CityIntentMetricsResponse(
        String window,
        OffsetDateTime from,
        List<CityIntentMetricRow> rows) {
}
