package com.suzhong.njl.web.admin.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suzhong.njl.domain.enums.ShelfStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 运营代建门店请求体（{@code POST /api/admin/merchants/{orgId}/stores}）。
 *
 * <p>新建门店强制绑定路径中的商家主体，{@code shelfStatus}、{@code homeRecommended}、
 * {@code homeSort} 不传时分别取 DRAFT、false、100；图集不传按空数组落库。
 * 校验失败或地区不存在/已停用返回 400（地区错误文案「地区不可用」），
 * 主体不存在返回 404；未知字段返回 400「请求体格式不正确」。
 *
 * @param name            门店名称，必填，最长 128；为空报「门店名称不能为空」，超长报「门店名称过长」
 * @param regionId        所属地区 ID，必填，必须是启用中的 {@code region} 节点；为空报「地区不能为空」
 * @param address         详细地址，可空，最长 256，超长报「地址过长」
 * @param phone           联系电话，可空，最长 32，超长报「电话过长」
 * @param coverUrl        封面图 URL，可空，最长 512，超长报「封面地址过长」
 * @param gallery         图集 URL 列表，可空；null 落库为空图集，元素内容不做 URL 校验
 * @param priceText       价格展示文案，可空，最长 64；只用于展示，不是结算价
 * @param priceFrom       起步价，单位元，可空；用于门户价格筛选与排序
 * @param packageText     套餐权威文案，可空；与 {@code store_package} 不一致时以本字段为准
 * @param facilities      设施说明，可空，纯文本
 * @param features        特色说明，可空，纯文本
 * @param notice          入住须知，可空，纯文本
 * @param trafficText     交通说明，可空，纯文本
 * @param shelfStatus     上下架状态，可空，默认 DRAFT；取值见 {@link ShelfStatus}：ON_SHELF、OFF_SHELF、DRAFT
 * @param homeRecommended 是否首页推荐，可空，默认 false
 * @param homeSort        首页推荐排序，可空，默认 100；范围 0 到 100000，越小越靠前，越界报「首页排序不能为负」/「首页排序过大」
 * @param testData        是否测试数据，可空，默认 false；抓取或导入的房源必须传 true
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(name = "AdminStoreCreateRequest", description = "运营代建门店请求体，用于 POST /api/admin/merchants/{orgId}/stores")
public record AdminStoreCreateRequest(
        @Schema(description = "门店名称，必填，最长 128；为空报「门店名称不能为空」，超长报「门店名称过长」")
        @NotBlank(message = "门店名称不能为空")
        @Size(max = 128, message = "门店名称过长")
        String name,
        @Schema(description = "所属地区 ID，必填，必须是启用中的 region 节点；为空报「地区不能为空」，地区不存在或已停用返回 400")
        @NotNull(message = "地区不能为空")
        Long regionId,
        @Schema(description = "详细地址，可空，最长 256，超长报「地址过长」")
        @Size(max = 256, message = "地址过长")
        String address,
        @Schema(description = "联系电话，可空，最长 32，超长报「电话过长」")
        @Size(max = 32, message = "电话过长")
        String phone,
        @Schema(description = "封面图 URL，可空，最长 512，超长报「封面地址过长」")
        @Size(max = 512, message = "封面地址过长")
        String coverUrl,
        @Schema(description = "图集 URL 列表，可空；null 落库为空图集，元素内容不做 URL 校验")
        List<String> gallery,
        @Schema(description = "价格展示文案，可空，最长 64；只用于展示，不是结算价")
        @Size(max = 64, message = "价格文案过长")
        String priceText,
        @Schema(description = "起步价，单位元，可空；用于门户价格筛选与排序")
        BigDecimal priceFrom,
        @Schema(description = "套餐权威文案，可空；与 store_package 不一致时以本字段为准")
        String packageText,
        @Schema(description = "设施说明，可空，纯文本")
        String facilities,
        @Schema(description = "特色说明，可空，纯文本")
        String features,
        @Schema(description = "入住须知，可空，纯文本")
        String notice,
        @Schema(description = "交通说明，可空，纯文本")
        String trafficText,
        @Schema(description = "上下架状态，可空，默认 DRAFT；取值 ON_SHELF、OFF_SHELF、DRAFT")
        ShelfStatus shelfStatus,
        @Schema(description = "是否首页推荐，可空，默认 false")
        Boolean homeRecommended,
        @Schema(description = "首页推荐排序，可空，默认 100；范围 0 到 100000，越小越靠前，越界报「首页排序不能为负」/「首页排序过大」")
        @Min(value = 0, message = "首页排序不能为负")
        @Max(value = 100000, message = "首页排序过大")
        Integer homeSort,
        @Schema(description = "是否测试数据，可空，默认 false；抓取或导入的房源必须传 true")
        Boolean testData) {
}
