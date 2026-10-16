package com.suzhong.njl.web.admin.dto;

import com.suzhong.njl.domain.enums.ReviewStatus;

public record ApproveMerchantApplyResponse(
        Long id,
        ReviewStatus status,
        Long merchantOrgId,
        Long merchantUserId,
        String phone,
        String plainPassword) {
}
