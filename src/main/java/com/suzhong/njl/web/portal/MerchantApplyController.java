package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.MerchantApplyService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.MerchantApplyRequest;
import com.suzhong.njl.web.portal.dto.MerchantApplyResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchant-applies")
public class MerchantApplyController {

    private final MerchantApplyService merchantApplyService;

    public MerchantApplyController(MerchantApplyService merchantApplyService) {
        this.merchantApplyService = merchantApplyService;
    }

    @PostMapping
    public ApiResponse<MerchantApplyResponse> submit(@Valid @RequestBody MerchantApplyRequest request) {
        return ApiResponse.ok(merchantApplyService.submit(request));
    }
}
