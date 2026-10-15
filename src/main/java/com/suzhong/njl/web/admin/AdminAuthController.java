package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.AdminAuthService;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.AdminLoginRequest;
import com.suzhong.njl.web.admin.dto.AdminLoginResponse;
import com.suzhong.njl.web.admin.dto.AdminMeResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminMeResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (user.userType() != UserType.OPERATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限");
        }
        return ApiResponse.ok(new AdminMeResponse(user.userId(), user.phone(), user.adminRole(), user.displayName()));
    }
}
