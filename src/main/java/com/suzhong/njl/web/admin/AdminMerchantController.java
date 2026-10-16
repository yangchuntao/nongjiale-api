package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.service.admin.AdminMerchantService;
import com.suzhong.njl.service.admin.AdminStoreService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminMerchantResponse;
import com.suzhong.njl.web.admin.dto.AdminStoreCreateRequest;
import com.suzhong.njl.web.admin.dto.AdminStoreResponse;
import com.suzhong.njl.web.admin.dto.MerchantStatusPatchRequest;
import com.suzhong.njl.web.admin.dto.PlainPasswordRequest;
import com.suzhong.njl.web.admin.dto.ResetPasswordResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/merchants")
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;
    private final AdminStoreService adminStoreService;

    public AdminMerchantController(AdminMerchantService adminMerchantService, AdminStoreService adminStoreService) {
        this.adminMerchantService = adminMerchantService;
        this.adminStoreService = adminStoreService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminMerchantResponse>> list(
            @RequestParam(required = false) MerchantOrgStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminMerchantService.list(status, page, pageSize));
    }

    @PatchMapping("/{orgId}")
    public ApiResponse<AdminMerchantResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long orgId,
            @Valid @RequestBody MerchantStatusPatchRequest request) {
        return ApiResponse.ok(adminMerchantService.updateStatus(AdminPrincipal.adminId(authentication), orgId, request));
    }

    @PostMapping("/{orgId}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(
            @PathVariable Long orgId,
            @Valid @RequestBody(required = false) PlainPasswordRequest request) {
        return ApiResponse.ok(adminMerchantService.resetPassword(orgId, request));
    }

    @PostMapping("/{orgId}/stores")
    public ApiResponse<AdminStoreResponse> createStore(
            @PathVariable Long orgId,
            @Valid @RequestBody AdminStoreCreateRequest request) {
        return ApiResponse.ok(adminStoreService.create(orgId, request));
    }
}
