package com.suzhong.njl.web.portal.dto;

import java.math.BigDecimal;

public record StorePackageItem(
        Long id,
        String name,
        BigDecimal priceFrom,
        String description,
        Integer sortOrder) {
}
