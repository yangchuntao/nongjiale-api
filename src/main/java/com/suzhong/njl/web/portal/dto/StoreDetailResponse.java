package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;
import java.util.List;

import com.suzhong.njl.domain.enums.ShelfStatus;

public record StoreDetailResponse(
        Long id,
        String name,
        Long regionId,
        String regionName,
        String address,
        String phone,
        String coverUrl,
        List<String> gallery,
        String priceText,
        BigDecimal priceFrom,
        String packageText,
        List<StorePackageItem> packages,
        String facilities,
        String features,
        String notice,
        String trafficText,
        ShelfStatus shelfStatus,
        Boolean homeRecommended,
        Integer homeSort,
        Boolean testData,
        String testTag,
        Long version) {
}
