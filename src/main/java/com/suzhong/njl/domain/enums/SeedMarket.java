package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 地区所属的种子市场。
 *
 * <p>落在 {@code region.seed_market} 列，存 {@code @EnumValue} 标注的 {@code code}。
 * 当前只有苏中一个市场，用于区分不同批次的种子地区数据，不参与门户可见性判断；
 * 前端如需展示市场名取 {@link #label()}。
 */
public enum SeedMarket {
    /** 苏中：江苏中部片区，当前种子数据所属市场。 */
    SUZHONG("SUZHONG", "苏中");

    @EnumValue
    private final String code;
    private final String label;

    SeedMarket(String code, String label) {
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
