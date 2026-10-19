package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 预订意向的跟进状态机。
 *
 * <p>落在 {@code booking_intent.status} 列，存 {@code @EnumValue} 标注的 {@code code}；
 * 门户提交时固定为 {@code PENDING}，之后只能由运营在
 * {@code POST /api/admin/intents/{id}/status} 迁移，并写一条 {@code STATUS_CHANGE} 备注。
 * 前端展示中文取 {@link #label()}。迁移规则集中在 {@link #canTransitTo(IntentStatus)}，
 * 越级或终态再改一律视为 409。
 */
public enum IntentStatus {
    /** 待跟进：游客刚提交，尚未与商家确认。可迁往已确认、已改期、已关闭。 */
    PENDING("PENDING", "待跟进"),
    /** 已确认：已与游客敲定到店安排。可迁往已入住、已关闭。 */
    CONFIRMED("CONFIRMED", "已确认"),
    /** 已改期：游客改过到店日期，需重新确认。只能迁往已确认或已关闭。 */
    RESCHEDULED("RESCHEDULED", "已改期"),
    /** 已入住：终态，表示游客实际到店；不能再迁往任何状态。 */
    CHECKED_IN("CHECKED_IN", "已入住"),
    /** 已关闭：终态，取消或无效意向；不能再迁往任何状态。 */
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

    /** 是否为终态：已入住和已关闭都不可再迁移。 */
    public boolean terminal() {
        return this == CHECKED_IN || this == CLOSED;
    }

    /**
     * 合法迁移：PENDING → CONFIRMED | RESCHEDULED | CLOSED；
     * CONFIRMED → CHECKED_IN | CLOSED；RESCHEDULED → CONFIRMED | CLOSED。
     * 终态不可再迁。
     *
     * <p>{@code target} 为 null 或与当前状态相同时返回 false，不抛异常；调用方据此返回 409。
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
