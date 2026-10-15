package com.suzhong.njl.web.merchant;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.merchant.MerchantStoreService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantStoreResponse;
import com.suzhong.njl.web.merchant.dto.MerchantStoreUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchant/stores")
public class MerchantStoreController {

    private final MerchantStoreService merchantStoreService;

    public MerchantStoreController(MerchantStoreService merchantStoreService) {
        this.merchantStoreService = merchantStoreService;
    }

    @GetMapping
    public ApiResponse<PageResult<MerchantStoreResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(merchantStoreService.list(merchant(authentication), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<MerchantStoreResponse> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(merchantStoreService.detail(merchant(authentication), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MerchantStoreResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MerchantStoreUpdateRequest request) {
        return ApiResponse.ok(merchantStoreService.update(merchant(authentication), id, request));
    }

    private AuthenticatedUser merchant(Authentication authentication) {
        return CurrentUser.require(authentication, UserType.MERCHANT);
    }
}
