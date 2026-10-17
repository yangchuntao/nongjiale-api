package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.suzhong.njl.domain.enums.RegionOpenStatus;

public record CityDetailResponse(
        Long id,
        String provinceName,
        String name,
        String slug,
        RegionOpenStatus openStatus,
        long storeCount,
        long articleCount,
        LocalDateTime updatedAt,
        Boolean enabled,
        int minStores,
        int minArticles,
        CityChecklistView checklist,
        boolean canOpen,
        boolean canClose,
        List<String> missingReasons,
        long version,
        Long updatedBy) {
}
