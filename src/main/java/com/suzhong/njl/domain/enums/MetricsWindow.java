package com.suzhong.njl.domain.enums;

import java.time.Clock;
import java.time.OffsetDateTime;

public enum MetricsWindow {
    DAYS_7("7", 7),
    DAYS_30("30", 30),
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
     */
    public OffsetDateTime from(Clock clock) {
        if (days == null) {
            return null;
        }
        return OffsetDateTime.now(clock).minusDays(days);
    }

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
