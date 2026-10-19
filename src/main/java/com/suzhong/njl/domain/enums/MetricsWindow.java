package com.suzhong.njl.domain.enums;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * 城市经营指标的统计时间窗。
 *
 * <p>不是数据库枚举：只作 {@code GET /api/admin/metrics/city-intents}
 * 及其导出接口的 {@code window} 查询参数，取值为 {@code 7}、{@code 30}、{@code ALL}。
 * 统计按滚动 24 小时计算，不是自然日零点；导出与列表使用同一个窗口。
 */
public enum MetricsWindow {
    /** 最近 7 天（滚动 168 小时）。 */
    DAYS_7("7", 7),
    /** 最近 30 天（滚动 720 小时）。 */
    DAYS_30("30", 30),
    /** 全部历史，没有时间下界。 */
    ALL("ALL", null);

    private final String code;
    private final Integer days;

    MetricsWindow(String code, Integer days) {
        this.code = code;
        this.days = days;
    }

    public String code() {
        return code;
    }

    /**
     * 滚动 24 小时，不是自然日零点。ALL 没有下界。
     *
     * @return 统计起始时间；{@code ALL} 返回 null，表示不加时间条件
     */
    public OffsetDateTime from(Clock clock) {
        if (days == null) {
            return null;
        }
        return OffsetDateTime.now(clock).minusDays(days);
    }

    /**
     * 解析 {@code window} 参数。
     *
     * <p>null 或空白按默认 {@code 7} 处理；无法识别的值抛 {@link IllegalArgumentException}
     * （消息为「参数不正确」），由控制层转成 400，不返回 null。
     */
    public static MetricsWindow parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DAYS_7;
        }
        String text = raw.trim();
        for (MetricsWindow window : values()) {
            if (window.code.equals(text)) {
                return window;
            }
        }
        throw new IllegalArgumentException("参数不正确");
    }
}
