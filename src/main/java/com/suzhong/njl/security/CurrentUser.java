package com.suzhong.njl.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.web.ApiException;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedUser require(Authentication authentication, UserType userType) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (user.userType() != userType) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限");
        }
        return user;
    }
}
