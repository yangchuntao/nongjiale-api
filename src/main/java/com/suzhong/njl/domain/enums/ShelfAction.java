package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 商家提交的上下架申请动作。
 *
 * <p>落在 {@code shelf_application.action} 列，存 {@code @EnumValue} 标注的 {@code code}。
 * 与 {@link ReviewStatus} 组合成申请单：待审期间不改变门店；
 * 通过后 {@code PUBLISH} 把门店置为 {@code ON_SHELF}，{@code UNPUBLISH} 置为 {@code OFF_SHELF}。
 * 运营代改上下架不走本枚举，直接写门店并记 {@code SHELF_FORCE} 审计。
 */
public enum ShelfAction {
    /** 申请上架：通过后门店进入 {@code ON_SHELF}，门户可见。 */
    PUBLISH("PUBLISH", "申请上架"),
    /** 申请下架：通过后门店进入 {@code OFF_SHELF}，门户详情返回 410。 */
    UNPUBLISH("UNPUBLISH", "申请下架");

    @EnumValue
    private final String code;
    private final String label;

    ShelfAction(String code, String label) {
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
