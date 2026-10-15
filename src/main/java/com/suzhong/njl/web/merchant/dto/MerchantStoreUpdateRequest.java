package com.suzhong.njl.web.merchant.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 不含 homeRecommended、homeSort、shelfStatus。这些字段出现在请求体里会被拒绝。
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
