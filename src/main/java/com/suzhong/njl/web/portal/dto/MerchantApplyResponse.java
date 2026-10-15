package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.ReviewStatus;

public record MerchantApplyResponse(
        Long id,
        ReviewStatus status) {
}