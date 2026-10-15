package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;

import com.suzhong.njl.domain.enums.ShelfStatus;

public record StoreCard(
        Long id,
        String name,
        Long regionId,
        String regionName,
        String coverUrl,
        String priceText,
        BigDecimal priceFrom,
        Boolean homeRecommended,
        Integer homeSort,
        Boolean testData,
        String testTag,
        ShelfStatus shelfStatus) {
}
