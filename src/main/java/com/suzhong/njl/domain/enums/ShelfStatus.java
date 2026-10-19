package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 门店的上下架状态。
 *
 * <p>落在 {@code store.shelf_status} 列，存 {@code @EnumValue} 标注的 {@code code}。
 * 门户列表、详情、首页推荐统一要求 {@code ON_SHELF}，且所属市已开通；
 * 其他状态在详情接口一律按 410「已下架或不可用」处理。
 * 变更途径有两条：商家提交 {@link ShelfAction} 申请并由运营审批，
 * 或运营在 {@code PUT /api/admin/stores/{id}} 直接改（记 {@code SHELF_FORCE} 审计）。
 * 前端展示中文取 {@link #label()}。
 */
public enum ShelfStatus {
    /** 上架：门户列表与详情可见，可提交预订意向；仍受城市是否开通约束。 */
    ON_SHELF("ON_SHELF", "上架"),
    /** 下架：门户不展示，详情返回 410；商家可申请重新上架。 */
    OFF_SHELF("OFF_SHELF", "下架"),
    /** 草稿：运营代建门店未指定状态时的默认值，门户完全不可见。 */
    DRAFT("DRAFT", "草稿");

    @EnumValue
    private final String code;
    private final String label;

    ShelfStatus(String code, String label) {
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
