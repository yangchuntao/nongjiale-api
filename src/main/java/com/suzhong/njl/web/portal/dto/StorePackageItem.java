package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;

/**
 * 门店套餐项（{@code GET /api/stores/{id}} 响应中 {@code packages} 的元素）。
 *
 * <p>结构化套餐只是详情页的补充展示，门店套餐的权威文案是 {@code StoreDetailResponse.packageText}；
 * {@code store_package} 可以没有行，此时本列表为空，页面仍应展示 {@code packageText}。
 *
 * @param id          套餐 ID
 * @param name        套餐名，非空
 * @param priceFrom   套餐起步价，单位<b>元</b>（不是分），可空
 * @param description 套餐说明，可空
 * @param sortOrder   排序号，非空；服务端按排序号升序、ID 升序返回
 */
public record StorePackageItem(
        Long id,
        String name,
        BigDecimal priceFrom,
        String description,
        Integer sortOrder) {
}
