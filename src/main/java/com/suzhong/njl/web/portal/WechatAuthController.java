package com.suzhong.njl.web.portal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.portal.WechatAuthService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.WechatAuthResponse;
import com.suzhong.njl.web.portal.dto.WechatBindRequest;
import com.suzhong.njl.web.portal.dto.WechatLinkResponse;
import com.suzhong.njl.web.portal.dto.WechatLoginRequest;

@RestController
@RequestMapping("/api/auth/wechat")
public class WechatAuthController {

    private final WechatAuthService wechatAuthService;

    public WechatAuthController(WechatAuthService wechatAuthService) {
        this.wechatAuthService = wechatAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<WechatAuthResponse> login(@RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(wechatAuthService.login(request));
    }

    @PostMapping("/bind")
    public ApiResponse<WechatAuthResponse> bind(Authentication authentication, @RequestBody WechatBindRequest request) {
        return ApiResponse.ok(wechatAuthService.bind(CurrentUser.require(authentication, UserType.TOURIST), request));
    }

    @PostMapping("/unbind")
    public ApiResponse<WechatLinkResponse> unbind(Authentication authentication) {
        return ApiResponse.ok(wechatAuthService.unbind(CurrentUser.require(authentication, UserType.TOURIST)));
    }

    @GetMapping("/link")
    public ApiResponse<WechatLinkResponse> link(Authentication authentication) {
        return ApiResponse.ok(wechatAuthService.link(CurrentUser.require(authentication, UserType.TOURIST)));
    }
}
