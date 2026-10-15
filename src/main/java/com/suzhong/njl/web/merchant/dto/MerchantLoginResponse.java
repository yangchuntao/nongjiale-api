package com.suzhong.njl.web.merchant.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

public record MerchantLoginResponse(
        String token,
        Instant expiresAt,
        Long merchantUserId,
        Long merchantOrgId,
        UserType role,
        String displayName) {
}
