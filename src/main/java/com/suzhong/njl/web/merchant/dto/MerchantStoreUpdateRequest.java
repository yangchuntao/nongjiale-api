package com.suzhong.njl.web.merchant.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商家更新门店的请求体（{@code PUT /api/merchant/stores/{id}}，须商家 Bearer）。
 *
 * <p>不含 homeRecommended、homeSort、shelfStatus。这些字段出现在请求体里会被拒绝。
 *
 * <p>可改字段白名单：名称、地区、地址、电话、封面、相册、价格文案、起步价、套餐文案、设施、特色、
 * 须知、交通。此外 {@code testData}、{@code merchantOrgId} 也不在白名单内，服务层写回前会按原值钉住，
 * 避免商家自行上下架或上首页推荐。上下架请走
 * {@code POST /api/merchant/stores/{id}/shelf-applications}。
 *
 * <p>所有字段按全量更新语义处理：未传的可空字段会被写成 null；{@code gallery} 例外，
 * 传 null 表示保持原相册不变，传空数组表示清空相册。未知字段一律拒绝。
 *
 * @param name        门店名，必填，最长 128 字
 * @param regionId    所属地区 ID，必填；地区必须存在且 {@code enabled=true}，否则 400「地区不可用」
 * @param address     地址，可空，最长 256 字
 * @param phone       联系电话，可空，最长 32 字；不做手机号格式校验
 * @param coverUrl    封面图 URL，可空，最长 512 字
 * @param gallery     相册图片 URL 列表，可空；null 保持原值，空数组清空
 * @param priceText   价格展示文案，可空，最长 64 字
 * @param priceFrom   起步价，可空，单位<b>元</b>（不是分）
 * @param packageText 套餐权威文案，可空，无长度限制；与结构化套餐不一致时以此为准
 * @param facilities  设施说明，可空
 * @param features    特色说明，可空
 * @param notice      预订须知，可空
 * @param trafficText 交通指引，可空
 * @param version     乐观锁版本号，必填，必须回传查询时的值；不匹配返回 409「数据已被修改」
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record MerchantStoreUpdateRequest(
        @NotBlank(message = "门店名称不能为空")
        @Size(max = 128, message = "门店名称过长")
        String name,
        @NotNull(message = "地区不能为空")
        Long regionId,
        @Size(max = 256, message = "地址过长")
        String address,
        @Size(max = 32, message = "电话过长")
        String phone,
        @Size(max = 512, message = "封面地址过长")
        String coverUrl,
        List<String> gallery,
        @Size(max = 64, message = "价格文案过长")
        String priceText,
        BigDecimal priceFrom,
        String packageText,
        String facilities,
        String features,
        String notice,
        String trafficText,
        @NotNull(message = "版本不能为空")
        Long version) {
}
