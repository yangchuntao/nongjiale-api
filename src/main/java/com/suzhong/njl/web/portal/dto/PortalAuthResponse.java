package com.suzhong.njl.web.portal.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

public record PortalAuthResponse(
        String token,
        Instant expiresAt,
        Long userId,
        UserType role,
        String displayName) {
}
