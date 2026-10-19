package com.suzhong.njl.service.city;

import java.util.List;

import com.suzhong.njl.domain.enums.DestinationStatus;

/**
 * 门户门店列表的地区过滤范围与目的地状态。
 *
 * <p>由 {@link CityVisibility#listScope(Long)} 产出，服务层只负责把它塞进 SQL 和响应：
 * {@code regionIds} 为空表示「按可见性算下来没有可查的地区」，此时不必查库，直接返回空页。
 *
 * @param regionIds         允许查询的地区 ID（市本身 + 其下启用区县），可能为空列表
 * @param destinationStatus 对客的目的地状态：OPEN 已开通、CLOSED 已关闭、NONE 未按市筛选或省
 * @param emptyHint         仅 CLOSED 时的空态提示，其余为 null
 */
public record StoreListingScope(List<Long> regionIds, DestinationStatus destinationStatus, String emptyHint) {

    /**
     * 构造「目的地未开通」的空范围。
     *
     * <p>固定空列表 + CLOSED + 「该目的地暂未开通」，保证市与区县两条关闭路径文案一致。
     *
     * @return 空范围
     */
    public static StoreListingScope closed() {
        return new StoreListingScope(List.of(), DestinationStatus.CLOSED, CityRules.EMPTY_HINT);
    }
}
