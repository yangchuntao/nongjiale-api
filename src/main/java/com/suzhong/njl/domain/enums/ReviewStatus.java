package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 审核状态，商家入驻申请和上下架申请共用。
 *
 * <p>分别落在 {@code merchant_apply.status} 与 {@code shelf_application.status} 列，
 * 两处都存 {@code @EnumValue} 标注的 {@code code}。迁移只有一次：
 * {@code PENDING} → {@code APPROVED} 或 {@code REJECTED}，后两者为终态。
 * 运营对非待审记录再审批返回 409；同一门店同时只允许一条待审申请。
 * 前端展示中文取 {@link #label()}。
 */
public enum ReviewStatus {
    /** 待审：已提交、等待运营处理；可迁往通过或驳回。 */
    PENDING("PENDING", "待审"),
    /** 已通过：入驻通过会开通商家账号，上下架通过会立即改写门店 {@code shelf_status}。终态。 */
    APPROVED("APPROVED", "已通过"),
    /** 已驳回：需记录驳回原因，业务数据（含门店可见性）保持不变。终态。 */
    REJECTED("REJECTED", "已驳回");

    @EnumValue
    private final String code;
    private final String label;

    ReviewStatus(String code, String label) {
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
