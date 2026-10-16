package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;

final class AdminPrincipal {

    private AdminPrincipal() {
    }

    static long adminId(Authentication authentication) {
        return CurrentUser.require(authentication, UserType.OPERATOR).userId();
    }
}
