package com.suzhong.njl.web.portal.dto;

import com.suzhong.njl.domain.enums.UserType;

public record PortalMeResponse(
        Long userId,
        String phone,
        String displayName,
        UserType role) {
}
