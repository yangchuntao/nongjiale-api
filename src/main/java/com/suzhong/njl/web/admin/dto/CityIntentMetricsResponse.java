package com.suzhong.njl.web.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CityIntentMetricsResponse(
        String window,
        OffsetDateTime from,
        List<CityIntentMetricRow> rows) {
}
