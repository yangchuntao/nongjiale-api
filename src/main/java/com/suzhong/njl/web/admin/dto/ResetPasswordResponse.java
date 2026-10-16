package com.suzhong.njl.web.admin.dto;

public record ResetPasswordResponse(
        Long merchantOrgId,
        Long merchantUserId,
        String phone,
        String plainPassword) {
}
