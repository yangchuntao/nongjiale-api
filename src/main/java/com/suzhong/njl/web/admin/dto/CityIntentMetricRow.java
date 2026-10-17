package com.suzhong.njl.web.admin.dto;

public record CityIntentMetricRow(
        Long cityId,
        String cityName,
        long intentCount,
        long confirmedCount,
        long checkedInCount,
        boolean total) {
}
