package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;

import com.suzhong.njl.domain.enums.ShelfStatus;

/**
 * 门店列表卡片（{@code GET /api/stores} 的 items 元素；{@code GET /api/home/feed} 的 stores 元素复用）。
 *
 * <p>列表口径：只含已上架（{@code ON_SHELF}）且所属市已开通的门店。测试数据（{@code testData=true}，
 * 抓取/导入房源）仍会出现在列表中，但不计入开城供给门槛，并通过 {@code testTag} 带「测试」标记。
 *
 * @param id              门店 ID
 * @param name            门店名
 * @param regionId        所属地区 ID（通常是区县或市）
 * @param regionName      所属地区展示名，可空；未开通地区或地区未启用时可能取不到名字
 * @param coverUrl        封面图 URL，可空；为空时前端用占位图
 * @param priceText       价格展示文案，例如「¥268 起」，可空；展示以该文案为准
 * @param priceFrom       起步价，单位<b>元</b>（不是分），可空；用于价格筛选/排序，不直接展示
 * @param homeRecommended 是否首页推荐；非空，列表接口原样带回，仅作展示
 * @param homeSort        首页推荐排序号，非空，数字越小越靠前；非推荐门店保留默认值（种子为 100），不能据此判断是否推荐
 * @param testData        是否为抓取/导入的测试数据，非空；true 时列表带「测试」标记
 * @param testTag         测试标记文案，固定「测试」；非测试数据为 null 而不是空串
 * @param shelfStatus     上下架状态，见 {@link com.suzhong.njl.domain.enums.ShelfStatus}；列表实际只会是 {@code ON_SHELF}
 */
public record StoreCard(
        Long id,
        String name,
        Long regionId,
        String regionName,
        String coverUrl,
        String priceText,
        BigDecimal priceFrom,
        Boolean homeRecommended,
        Integer homeSort,
        Boolean testData,
        String testTag,
        ShelfStatus shelfStatus) {
}
