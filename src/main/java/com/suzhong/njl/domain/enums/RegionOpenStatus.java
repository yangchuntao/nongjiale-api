package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 市的对客开通态。只对 {@link RegionLevel#CITY} 有意义。
 * 与意向状态 {@link IntentStatus#CLOSED} 不是同一个枚举。
 *
 * <p>落在 {@code region.open_status} 列，存 {@code @EnumValue} 标注的 {@code code}。
 * 省和区县该列为空；空值在 {@code CityRules.effective} 中一律按 {@code CLOSED} 处理。
 * 迁移只有运营的开城/关城两个动作，关城不删除历史数据；前端展示直接取 {@link #code()}。
 */
public enum RegionOpenStatus {
    /** 未开通：门户不展示该市房源，按市投放的 Banner 也不露出；库中为空时同样按此态对待。 */
    CLOSED("CLOSED"),
    /** 已开通：门户可查询该市及其区县的房源，开城门槛达标后才能迁入。 */
    OPEN("OPEN");

    @EnumValue
    private final String code;

    RegionOpenStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
