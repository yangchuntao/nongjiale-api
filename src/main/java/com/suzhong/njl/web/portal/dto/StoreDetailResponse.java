package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 门店详情（{@code GET /api/stores/{id}} 的响应体，公开接口）。
 *
 * <p>只有已上架且所属市已开通的门店可读：不存在返回 404「房源不存在」，已下架或城市未开通返回
 * 410「已下架或不可用」。套餐以 {@code packageText} 为权威文案，{@code packages} 只是结构化补充，
 * 两者不一致时以 {@code packageText} 为准；测试数据的标记规则与列表卡片一致。
 *
 * @param id              门店 ID
 * @param name            门店名
 * @param regionId        所属地区 ID
 * @param regionName      所属地区展示名，可空
 * @param address         门店地址，可空
 * @param phone           门店联系电话，可空；明文，不脱敏
 * @param coverUrl        封面图 URL，可空
 * @param gallery         相册图片 URL 列表，无相册或 JSON 解析失败时为空列表，不会返回 null
 * @param priceText       价格展示文案，可空
 * @param priceFrom       起步价，单位<b>元</b>（不是分），可空
 * @param packageText     套餐权威文案，可空；详情与验收以此为准，可包含换行等排版字符
 * @param packages        结构化套餐列表，可为空列表；按排序号、ID 升序，不能反推套餐文案
 * @param facilities      设施说明，可空；纯文本，前端按约定分隔符展示
 * @param features        特色说明，可空；纯文本
 * @param notice          预订须知，可空；纯文本
 * @param trafficText     交通指引，可空；纯文本
 * @param shelfStatus     上下架状态；详情能返回时实际只会是 {@code ON_SHELF}
 * @param homeRecommended 是否首页推荐
 * @param homeSort        首页推荐排序号，非空，数字越小越靠前；非推荐门店保留默认值
 * @param testData        是否为抓取/导入的测试数据，非空
 * @param testTag         测试标记文案，固定「测试」；非测试数据为 null
 * @param version         乐观锁版本号，非空，只读展示；商家/运营更新时需回传，冲突返回 409
 */
@Schema(name = "StoreDetailResponse", description = "门店详情，GET /api/stores/{id} 的响应体；只有已上架且所属市已开通的门店可读，套餐以 packageText 为权威文案")
public record StoreDetailResponse(
        @Schema(description = "门店 ID")
        Long id,
        @Schema(description = "门店名", example = "表弟店")
        String name,
        @Schema(description = "所属地区 ID")
        Long regionId,
        @Schema(description = "所属地区展示名，可空", example = "广陵区")
        String regionName,
        @Schema(description = "门店地址，可空", example = "扬州市广陵区运河东路 18 号")
        String address,
        @Schema(description = "门店联系电话，可空；明文，不脱敏", example = "13911110001")
        String phone,
        @Schema(description = "封面图 URL，可空")
        String coverUrl,
        @Schema(description = "相册图片 URL 列表，无相册或 JSON 解析失败时为空列表，不会返回 null")
        List<String> gallery,
        @Schema(description = "价格展示文案，可空", example = "¥268起")
        String priceText,
        @Schema(description = "起步价，单位元（不是分），可空", example = "268.00")
        BigDecimal priceFrom,
        @Schema(description = "套餐权威文案，可空；详情与验收以此为准，可包含换行等排版字符", example = "含双早与农家晚餐，可住两晚，周末不加价。")
        String packageText,
        @Schema(description = "结构化套餐列表，可为空列表；按排序号、ID 升序，不能反推套餐文案")
        List<StorePackageItem> packages,
        @Schema(description = "设施说明，可空；纯文本，前端按约定分隔符展示")
        String facilities,
        @Schema(description = "特色说明，可空；纯文本")
        String features,
        @Schema(description = "预订须知，可空；纯文本")
        String notice,
        @Schema(description = "交通指引，可空；纯文本")
        String trafficText,
        @Schema(description = "上下架状态；详情能返回时实际只会是 ON_SHELF")
        ShelfStatus shelfStatus,
        @Schema(description = "是否首页推荐")
        Boolean homeRecommended,
        @Schema(description = "首页推荐排序号，非空，数字越小越靠前；非推荐门店保留默认值")
        Integer homeSort,
        @Schema(description = "是否为抓取/导入的测试数据，非空")
        Boolean testData,
        @Schema(description = "测试标记文案，固定「测试」；非测试数据为 null", example = "测试")
        String testTag,
        @Schema(description = "乐观锁版本号，非空，只读展示；商家/运营更新时需回传，冲突返回 409")
        Long version) {
}
