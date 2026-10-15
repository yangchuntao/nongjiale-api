package com.suzhong.njl.security;

import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.domain.enums.UserType;

public record AuthenticatedUser(
        Long userId,
        UserType userType,
        AdminRole adminRole,
        Long merchantOrgId,
        String displayName,
        String phone) {

    public String authority() {
        if (userType == UserType.OPERATOR) {
            if (adminRole == null) {
                throw new IllegalStateException("运营身份缺少角色");
            }
            return adminRole.name();
        }
        return userType.name();
    }
}
