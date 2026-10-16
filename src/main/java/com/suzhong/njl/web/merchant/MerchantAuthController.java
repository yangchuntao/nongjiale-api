package com.suzhong.njl.web.merchant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.merchant.MerchantAuthService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.merchant.dto.MerchantLoginRequest;
import com.suzhong.njl.web.merchant.dto.MerchantLoginResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchant/auth")
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;

    public MerchantAuthController(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<MerchantLoginResponse> login(@Valid @RequestBody MerchantLoginRequest request) {
        return ApiResponse.ok(merchantAuthService.login(request));
    }
}
