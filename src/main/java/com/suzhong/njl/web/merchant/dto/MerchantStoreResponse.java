package com.suzhong.njl.web.merchant.dto;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;

public record MerchantStoreResponse(
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
