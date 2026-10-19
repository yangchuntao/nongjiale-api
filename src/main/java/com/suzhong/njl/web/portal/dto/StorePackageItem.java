package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(name = "StorePackageItem", description = "门店套餐项，GET /api/stores/{id} 响应中 packages 的元素；结构化补充展示，权威文案是 StoreDetailResponse.packageText")
public record StorePackageItem(
        @Schema(description = "套餐 ID")
        Long id,
        @Schema(description = "套餐名，非空", example = "标间")
        String name,
        @Schema(description = "套餐起步价，单位元（不是分），可空", example = "268.00")
        BigDecimal priceFrom,
        @Schema(description = "套餐说明，可空", example = "仅床位，不含餐")
        String description,
        @Schema(description = "排序号，非空；服务端按排序号升序、ID 升序返回")
        Integer sortOrder) {
}
