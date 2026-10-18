package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.BannerScope;

public record HomeBannerItem(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        BannerScope scope,
        Long cityId,
        String citySlug,
        String cityName,
        Integer sortOrder,
        Boolean enabled) {
}
