package com.suzhong.njl.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 首页 Banner 的投放范围。
 *
 * <p>落在 {@code home_banner.scope} 列，存 {@code @EnumValue} 标注的 {@code code}。
 * 与 {@code home_banner.city_id} 联动：{@code ALL} 时城市必须为空，{@code CITY} 时必须指向一个已开通的市。
 * 本枚举没有 label，门户/运营前端直接用 {@link #code()} 判断与展示。
 */
public enum BannerScope {
    /** 全站：所有访客可见，不受城市开通状态影响。 */
    ALL("ALL"),
    /** 指定市：仅该市当前处于开通状态时可见；关城后该 Banner 自动不展示。 */
    CITY("CITY");

    @EnumValue
    private final String code;

    BannerScope(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
