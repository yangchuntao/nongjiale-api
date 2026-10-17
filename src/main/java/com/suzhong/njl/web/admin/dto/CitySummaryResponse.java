package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.RegionOpenStatus;

public record CitySummaryResponse(
        Long id,
        String provinceName,
        String name,
        String slug,
        RegionOpenStatus openStatus,
        long storeCount,
        long articleCount,
        LocalDateTime updatedAt) {
}
