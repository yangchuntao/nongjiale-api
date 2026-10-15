package com.suzhong.njl.domain.enums;

public enum StoreSort {
    COMPREHENSIVE("comprehensive"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc");

    private final String param;

    StoreSort(String param) {
        this.param = param;
    }

    public String param() {
        return param;
    }

    public String orderBySql() {
        return switch (this) {
            case PRICE_ASC -> "s.price_from ASC, s.id ASC";
            case PRICE_DESC -> "s.price_from DESC, s.id ASC";
            case COMPREHENSIVE -> "s.home_sort ASC, s.id ASC";
        };
    }

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
