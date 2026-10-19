package com.suzhong.njl.domain.enums;

/**
 * 门户门店列表的排序方式。
 *
 * <p>不是数据库枚举：对应 {@code GET /api/stores} 的 {@code sort} 参数，
 * 取值 {@code comprehensive}、{@code price_asc}、{@code price_desc}；
 * 由 {@link #orderBySql()} 直接拼进 SQL 的 ORDER BY，因此新增取值必须同步改这里，禁止把用户输入原样拼 SQL。
 * 价格相同时统一用 {@code id} 升序兜底，保证分页稳定。
 */
public enum StoreSort {
    /** 综合排序：按首页排序值 {@code home_sort} 升序（默认 100，数字越小越前），同值按 {@code id} 升序；默认排序。 */
    COMPREHENSIVE("comprehensive"),
    /** 价格升序：按起步价升序，价格可为空，空值由数据库排序规则决定。 */
    PRICE_ASC("price_asc"),
    /** 价格降序：按起步价降序，同样以 {@code id} 升序兜底。 */
    PRICE_DESC("price_desc");

    private final String param;

    StoreSort(String param) {
        this.param = param;
    }

    public String param() {
        return param;
    }

    /** 返回该排序对应的 ORDER BY 片段，只使用受控的列名，不得拼接外部输入。 */
    public String orderBySql() {
        return switch (this) {
            case PRICE_ASC -> "s.price_from ASC, s.id ASC";
            case PRICE_DESC -> "s.price_from DESC, s.id ASC";
            case COMPREHENSIVE -> "s.home_sort ASC, s.id ASC";
        };
    }

    /**
     * 解析排序参数。
     *
     * <p>null 或空白返回 {@code COMPREHENSIVE}；无法识别的值抛 {@link IllegalArgumentException}
     * （消息为「排序方式不正确」），由调用方转成 400，不返回 null。
     */
    public static StoreSort fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMPREHENSIVE;
        }
        for (StoreSort sort : values()) {
            if (sort.param.equals(raw)) {
                return sort;
            }
        }
        throw new IllegalArgumentException("排序方式不正确");
    }
}
