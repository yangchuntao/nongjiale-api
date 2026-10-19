package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 地区行政层级。
 *
 * <p>落在 {@code region.level} 列（TINYINT），存 {@code @EnumValue} 标注的数字 {@code code}，
 * 不是枚举名。读取时由 {@code RegionLevelTypeHandler} 按数值转换，避免 H2 返回非 Integer 类型对不上。
 * 层级决定业务口径：只有市有开通态，区县按父市是否开通判断可见性。
 * 前端展示层级名取 {@link #label()}。
 */
public enum RegionLevel {
    /** 省：只用于聚合展示，不参与开城判断，{@code open_status} 保持空。 */
    PROVINCE(1, "省"),
    /** 市：开城的最小单位，拥有 {@code open_status}，门户只露出已开通市的房源。 */
    CITY(2, "市"),
    /** 区县：归属某个市，可见性跟随父市的开通状态。 */
    DISTRICT(3, "区县");

    @EnumValue
    private final int code;
    private final String label;

    RegionLevel(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    /**
     * 按数字编码反查层级。
     *
     * @param code 库中的 {@code region.level} 值
     * @return 对应层级
     * @throws IllegalArgumentException 编码不在 1 至 3 之间时抛出，不返回 null
     */
    public static RegionLevel fromCode(int code) {
        for (RegionLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知地区层级: " + code);
    }
}
