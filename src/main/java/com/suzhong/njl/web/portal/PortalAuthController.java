package com.suzhong.njl.web.portal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.portal.PortalAuthService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.PortalAuthResponse;
import com.suzhong.njl.web.portal.dto.PortalLoginRequest;
import com.suzhong.njl.web.portal.dto.PortalMeResponse;
import com.suzhong.njl.web.portal.dto.PortalRegisterRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    public PortalAuthController(PortalAuthService portalAuthService) {
        this.portalAuthService = portalAuthService;
    }

    @PostMapping("/register")
    public ApiResponse<PortalAuthResponse> register(@Valid @RequestBody PortalRegisterRequest request) {
        return ApiResponse.ok(portalAuthService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<PortalAuthResponse> login(@Valid @RequestBody PortalLoginRequest request) {
        return ApiResponse.ok(portalAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<PortalMeResponse> me(Authentication authentication) {
        return ApiResponse.ok(portalAuthService.me(CurrentUser.require(authentication, UserType.TOURIST)));
    }
}
