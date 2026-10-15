package com.suzhong.njl.web.merchant;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.merchant.MerchantShelfService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationRequest;
import com.suzhong.njl.web.merchant.dto.ShelfApplicationResponse;

import jakarta.validation.Valid;

@RestController
public class MerchantShelfController {

    private final MerchantShelfService merchantShelfService;

    public MerchantShelfController(MerchantShelfService merchantShelfService) {
        this.merchantShelfService = merchantShelfService;
    }

    @PostMapping("/api/merchant/stores/{id}/shelf-applications")
    public ApiResponse<ShelfApplicationResponse> apply(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ShelfApplicationRequest request) {
        return ApiResponse.ok(merchantShelfService.apply(
                CurrentUser.require(authentication, UserType.MERCHANT), id, request));
    }

    @GetMapping("/api/merchant/shelf-applications")
    public ApiResponse<PageResult<ShelfApplicationResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(merchantShelfService.list(
                CurrentUser.require(authentication, UserType.MERCHANT), page, pageSize));
    }
}
