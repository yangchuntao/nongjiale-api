package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.domain.enums.ShelfAction;

public record ShelfApplicationAdminItem(
        Long id,
        Long storeId,
        String storeName,
        Long merchantOrgId,
        ShelfAction action,
        String reason,
        ReviewStatus status,
        String rejectReason,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt) {
}
