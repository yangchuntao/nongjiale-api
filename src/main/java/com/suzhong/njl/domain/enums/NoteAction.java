package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum NoteAction {
    STATUS_CHANGE("STATUS_CHANGE", "状态变更"),
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
