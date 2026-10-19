package com.suzhong.njl.web.admin.dto;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;

/**
 * 运营端门店详情（{@code GET /api/admin/stores} 的 items 元素、
 * {@code GET /api/admin/stores/{id}}、代建与代编接口的 data）。
 *
 * <p>与门户卡片不同，这里返回完整文案、内部状态与版本号，供后台表单直接回填；
 * 图集从库内 JSON 字符串解析，解析失败按空列表兜底，不影响其他字段。
 * 时间字段不入本结构，避免后台表格展示冗余时间。
 *
 * @param id              门店 ID
 * @param merchantOrgId   所属商家主体 ID
 * @param regionId        所属地区 ID
 * @param name            门店名称
 * @param address         详细地址，可空
 * @param phone           联系电话，可空，后台不脱敏
 * @param coverUrl        封面图 URL，可空
 * @param gallery         图集 URL 列表，无图或 JSON 解析失败时为空列表
 * @param priceText       价格展示文案，可空
 * @param priceFrom       起步价，单位元，可空
 * @param packageText     套餐权威文案，可空
 * @param facilities      设施说明，可空
 * @param features        特色说明，可空
 * @param notice          入住须知，可空
 * @param trafficText     交通说明，可空
 * @param shelfStatus     上下架状态，取值见 {@link ShelfStatus}：ON_SHELF、OFF_SHELF、DRAFT
 * @param homeRecommended 是否首页推荐
 * @param homeSort        首页推荐排序，越小越靠前
 * @param testData        是否为测试数据；true 时门户展示「测试」标记
 * @param version         乐观锁版本号，代编时必须原样回传
 */
public record AdminStoreResponse(
        Long id,
        Long merchantOrgId,
        Long regionId,
        String name,
        String address,
        String phone,
        String coverUrl,
        List<String> gallery,
        String priceText,
        BigDecimal priceFrom,
        String packageText,
        String facilities,
        String features,
        String notice,
        String trafficText,
        ShelfStatus shelfStatus,
        Boolean homeRecommended,
        Integer homeSort,
        Boolean testData,
        Long version) {
}
