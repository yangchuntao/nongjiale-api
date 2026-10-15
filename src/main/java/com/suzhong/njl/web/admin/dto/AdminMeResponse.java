package com.suzhong.njl.web.admin.dto;

import com.suzhong.njl.domain.enums.AdminRole;

public record AdminMeResponse(
        Long adminId,
        String phone,
        AdminRole role,
        String displayName) {
}
