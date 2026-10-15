package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.SeedMarket;

public record RegionItem(
        Long id,
        Long parentId,
        String slug,
        String name,
        RegionLevel level,
        Integer sortOrder,
        SeedMarket seedMarket) {
}
