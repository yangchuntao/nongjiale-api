package com.suzhong.njl.web.merchant.dto;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家端门店（{@code GET /api/merchant/stores} 的 items 元素，以及
 * {@code GET/PUT /api/merchant/stores/{id}} 的响应体）。
 *
 * <p>只返回本商家主体名下的门店，按 ID 升序。其中 {@code shelfStatus}、{@code homeRecommended}、
 * {@code homeSort}、{@code testData} 在本端<b>只读</b>：上下架要走申请审批，首页推荐与测试标记由运营维护；
 * 商家更新门店时这些字段会被服务层按原值钉住，不能通过 PUT 修改。
 *
 * @param id              门店 ID
 * @param merchantOrgId   所属商家主体 ID，即 {@code merchant_org.id}
 * @param regionId        所属地区 ID
 * @param name            门店名
 * @param address         门店地址，可空
 * @param phone           门店联系电话，可空
 * @param coverUrl        封面图 URL，可空
 * @param gallery         相册图片 URL 列表；无相册或 JSON 解析失败时为空列表，不会返回 null
 * @param priceText       价格展示文案，可空
 * @param priceFrom       起步价，单位<b>元</b>（不是分），可空
 * @param packageText     套餐权威文案，可空；与 {@code store_package} 不一致时以此为准
 * @param facilities      设施说明，可空
 * @param features        特色说明，可空
 * @param notice          预订须知，可空
 * @param trafficText     交通指引，可空
 * @param shelfStatus     上下架状态，见 {@link com.suzhong.njl.domain.enums.ShelfStatus}；只读，需变更请提交申请
 * @param homeRecommended 是否首页推荐，非空；只读，商家不能自行修改
 * @param homeSort        首页推荐排序号，非空；只读，数字越小越靠前
 * @param testData        是否为抓取/导入的测试数据，非空；只读，由运营维护，门户会带「测试」标记
 * @param version         乐观锁版本号，非空；PUT 更新时必须原样回传，冲突返回 409「数据已被修改」
 */
@Schema(name = "MerchantStoreResponse", description = "商家端门店；只返回本商家主体名下的门店，上下架/首页推荐/排序/测试标记在商家端只读")
public record MerchantStoreResponse(
        @Schema(description = "门店 ID")
        Long id,
        @Schema(description = "所属商家主体 ID，即 merchant_org.id")
        Long merchantOrgId,
        @Schema(description = "所属地区 ID")
        Long regionId,
        @Schema(description = "门店名")
        String name,
        @Schema(description = "门店地址，可空")
        String address,
        @Schema(description = "门店联系电话，可空")
        String phone,
        @Schema(description = "封面图 URL，可空")
        String coverUrl,
        @Schema(description = "相册图片 URL 列表；无相册或解析失败时为空列表，不会返回 null")
        List<String> gallery,
        @Schema(description = "价格展示文案，可空")
        String priceText,
        @Schema(description = "起步价，单位元（不是分），可空")
        BigDecimal priceFrom,
        @Schema(description = "套餐权威文案，可空；与结构化套餐不一致时以此为准")
        String packageText,
        @Schema(description = "设施说明，可空")
        String facilities,
        @Schema(description = "特色说明，可空")
        String features,
        @Schema(description = "预订须知，可空")
        String notice,
        @Schema(description = "交通指引，可空")
        String trafficText,
        @Schema(description = "上下架状态；只读，需变更请提交上下架申请")
        ShelfStatus shelfStatus,
        @Schema(description = "是否首页推荐，非空；只读，商家不能自行修改")
        Boolean homeRecommended,
        @Schema(description = "首页推荐排序号，非空；只读，数字越小越靠前")
        Integer homeSort,
        @Schema(description = "是否为抓取/导入的测试数据，非空；只读，由运营维护，门户会带「测试」标记")
        Boolean testData,
        @Schema(description = "乐观锁版本号，非空；PUT 更新时必须原样回传，冲突返回 409「数据已被修改」")
        Long version) {
}
