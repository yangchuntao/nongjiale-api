package com.suzhong.njl.web.admin.dto;

import com.suzhong.njl.domain.enums.ReviewStatus;

public record RejectMerchantApplyResponse(
        Long id,
        ReviewStatus status,
        String rejectReason) {
}
