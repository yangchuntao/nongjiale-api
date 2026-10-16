package com.suzhong.njl.web.merchant;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.merchant.MerchantIntentService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantIntentResponse;

@RestController
public class MerchantIntentController {

    private final MerchantIntentService merchantIntentService;

    public MerchantIntentController(MerchantIntentService merchantIntentService) {
        this.merchantIntentService = merchantIntentService;
    }

    @GetMapping("/api/merchant/intents")
    public ApiResponse<PageResult<MerchantIntentResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(merchantIntentService.list(
                CurrentUser.require(authentication, UserType.MERCHANT), page, pageSize));
    }

    @GetMapping("/api/merchant/intents/{id}")
    public ApiResponse<MerchantIntentResponse> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(merchantIntentService.detail(
                CurrentUser.require(authentication, UserType.MERCHANT), id));
    }
}
