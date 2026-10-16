package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.ReviewStatus;
import com.suzhong.njl.service.admin.AdminMerchantApplyService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ApproveMerchantApplyResponse;
import com.suzhong.njl.web.admin.dto.MerchantApplyItem;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.RejectMerchantApplyResponse;
import com.suzhong.njl.web.admin.dto.RejectReasonRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/merchant-applies")
public class AdminMerchantApplyController {

    private final AdminMerchantApplyService adminMerchantApplyService;

    public AdminMerchantApplyController(AdminMerchantApplyService adminMerchantApplyService) {
        this.adminMerchantApplyService = adminMerchantApplyService;
    }

    @GetMapping
    public ApiResponse<PageResult<MerchantApplyItem>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminMerchantApplyService.list(status, page, pageSize));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ApproveMerchantApplyResponse> approve(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PlainPasswordRequest request) {
        return ApiResponse.ok(adminMerchantApplyService.approve(AdminPrincipal.adminId(authentication), id, request));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<RejectMerchantApplyResponse> reject(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectReasonRequest request) {
        return ApiResponse.ok(adminMerchantApplyService.reject(AdminPrincipal.adminId(authentication), id, request));
    }
}
