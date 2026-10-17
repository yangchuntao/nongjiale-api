package com.suzhong.njl.web.portal.dto;

import java.util.List;

import com.suzhong.njl.domain.enums.DestinationStatus;

public record StoreListResponse(
        List<StoreCard> items,
        long total,
        int page,
        int pageSize,
        DestinationStatus destinationStatus,
        String emptyHint) {
}
