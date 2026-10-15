package com.suzhong.njl.web.merchant.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.domain.enums.ShelfAction;

public record ShelfApplicationResponse(
        Long id,
        Long storeId,
        String storeName,
        Long merchantOrgId,
        ShelfAction action,
        String reason,
        ReviewStatus status,
        String rejectReason,
        LocalDateTime createdAt) {
}
