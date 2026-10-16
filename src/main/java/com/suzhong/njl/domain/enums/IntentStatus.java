package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum IntentStatus {
    PENDING("PENDING", "待跟进"),
    CONFIRMED("CONFIRMED", "已确认"),
    RESCHEDULED("RESCHEDULED", "已改期"),
    CHECKED_IN("CHECKED_IN", "已入住"),
    CLOSED("CLOSED", "已关闭");

    @EnumValue
    private final String code;
    private final String label;

    IntentStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean terminal() {
        return this == CHECKED_IN || this == CLOSED;
    }

    /**
     * 合法迁移：PENDING → CONFIRMED | RESCHEDULED | CLOSED；
     * CONFIRMED → CHECKED_IN | CLOSED；RESCHEDULED → CONFIRMED | CLOSED。
     * 终态不可再迁。
     */
    public boolean canTransitTo(IntentStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == RESCHEDULED || target == CLOSED;
            case CONFIRMED -> target == CHECKED_IN || target == CLOSED;
            case RESCHEDULED -> target == CONFIRMED || target == CLOSED;
            case CHECKED_IN, CLOSED -> false;
        };
    }
}
