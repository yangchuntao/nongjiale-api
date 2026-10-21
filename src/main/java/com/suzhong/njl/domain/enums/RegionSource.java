package com.suzhong.njl.domain.enums;

/**
 * 地区数据来源。
 *
 * <p>落在 {@code region.source} 列，存字符串码而非枚举序号，便于人工排查和后续扩展。
 * 用途：区分「迁移脚本写入的种子区划」与「通过区划同步接口导入的全国区划」，
 * 排查数据问题时可快速定位来源；不参与任何对客可见性判断。
 */
public enum RegionSource {

    /** 迁移脚本写入的种子地区（苏中、苏州）。 */
    SEED("SEED", "种子数据"),

    /** 通过区划同步接口或 CSV 导入的全国行政区划。 */
    SYNC("SYNC", "区划同步");

    private final String code;
    private final String label;

    RegionSource(String code, String label) {
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
