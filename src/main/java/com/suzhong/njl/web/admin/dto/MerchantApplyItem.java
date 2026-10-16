package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ReviewStatus;

public record MerchantApplyItem(
        Long id,
        String orgName,
        String contactName,
        String contactPhone,
        String plannedStoreName,
        Long regionId,
        String description,
        String licenseUrls,
        ReviewStatus status,
        String rejectReason,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long merchantOrgId,
        LocalDateTime createdAt) {
}
