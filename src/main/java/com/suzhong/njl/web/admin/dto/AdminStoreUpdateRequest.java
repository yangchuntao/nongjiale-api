package com.suzhong.njl.web.admin.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.ShelfStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 运营代编门店请求体（{@code PUT /api/admin/stores/{id}}）。
 *
 * <p>整单保存：正文、地址、首页推荐、上下架状态都会覆盖原值；{@code gallery} 为 null 时保留原图集，
 * {@code testData} 为 null 时保留原值。{@code shelfStatus} 与当前值不同即视为代上下架，
 * 立即生效并写 {@code SHELF_FORCE} 审计，不走申请队列。
 * 版本号不一致返回 409「数据已被修改」，门店不存在返回 404；
 * 字段名是 {@code homeRecommended}、{@code homeSort}，不是 homeRecommendSort。
 *
 * @param name            门店名称，必填，最长 128；为空报「门店名称不能为空」，超长报「门店名称过长」
 * @param regionId        所属地区 ID，必填，必须是启用中的地区；为空报「地区不能为空」，不可用报 400「地区不可用」
 * @param address         详细地址，可空，最长 256
 * @param phone           联系电话，可空，最长 32
 * @param coverUrl        封面图 URL，可空，最长 512
 * @param gallery         图集 URL 列表，可空；传 null 表示保留原图集，传空数组表示清空
 * @param priceText       价格展示文案，可空，最长 64
 * @param priceFrom       起步价，单位元，可空
 * @param packageText     套餐权威文案，可空
 * @param facilities      设施说明，可空
 * @param features        特色说明，可空
 * @param notice          入住须知，可空
 * @param trafficText     交通说明，可空
 * @param homeRecommended 是否首页推荐，必填；为空报「首页推荐不能为空」
 * @param homeSort        首页推荐排序，必填，范围 0 到 100000；为空报「首页排序不能为空」，越界报排序错误
 * @param shelfStatus     上下架状态，必填，取值见 {@link ShelfStatus}；为空报「上下架状态不能为空」
 * @param testData        是否测试数据，可空；null 表示保留原值
 * @param version         乐观锁版本号，必填；为空报「版本不能为空」，与库内不一致返回 409
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AdminStoreUpdateRequest(
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
        @NotNull(message = "首页推荐不能为空")
        Boolean homeRecommended,
        @NotNull(message = "首页排序不能为空")
        @Min(value = 0, message = "首页排序不能为负")
        @Max(value = 100000, message = "首页排序过大")
        Integer homeSort,
        @NotNull(message = "上下架状态不能为空")
        ShelfStatus shelfStatus,
        Boolean testData,
        @NotNull(message = "版本不能为空")
        Long version) {
}
