package com.suzhong.njl.web.admin.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.AdminRole;

public record AdminLoginResponse(
        String token,
        Instant expiresAt,
        Long adminId,
        AdminRole role,
        String displayName) {
}
