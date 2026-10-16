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
 * 代编整单保存。shelfStatus 与当前值不同即代上下架，不走申请队列。
 * 字段名是 homeRecommended、homeSort，不是 homeRecommendSort。
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
