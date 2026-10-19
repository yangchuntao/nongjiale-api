package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 预订意向跟进记录的动作类型。
 *
 * <p>落在 {@code intent_note.action} 列，存 {@code @EnumValue} 标注的 {@code code}。
 * 与 {@code from_status}、{@code to_status} 配合：状态变更为 {@code STATUS_CHANGE}，
 * 纯文字备注为 {@code COMMENT}。前端按 {@link #label()} 区分并展示时间线。
 */
public enum NoteAction {
    /** 状态变更：由运营改意向状态时自动写入，带前后状态与可选说明。 */
    STATUS_CHANGE("STATUS_CHANGE", "状态变更"),
    /** 备注：运营只追加文字，不改变意向状态。 */
    COMMENT("COMMENT", "备注");

    @EnumValue
    private final String code;
    private final String label;

    NoteAction(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }
}
