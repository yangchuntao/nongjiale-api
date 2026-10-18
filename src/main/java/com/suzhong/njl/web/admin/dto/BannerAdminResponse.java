package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.BannerScope;

public record BannerAdminResponse(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        BannerScope scope,
        Long cityId,
        String citySlug,
        String cityName,
        Boolean cityOpen,
        Integer sortOrder,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
