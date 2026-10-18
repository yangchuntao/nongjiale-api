package com.suzhong.njl.web.portal.dto;

import java.time.Instant;

import com.suzhong.njl.domain.enums.UserType;

public record WechatAuthResponse(
        String token,
        Instant expiresAt,
        Long userId,
        UserType role,
        String displayName,
        String phone,
        boolean phoneBound,
        boolean wechatBound,
        boolean accountLinked) {
}
