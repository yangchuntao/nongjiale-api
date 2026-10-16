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
import com.suzhong.njl.service.admin.AdminShelfService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.RejectReasonRequest;
import com.suzhong.njl.web.admin.dto.ShelfApplicationAdminItem;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/shelf-applications")
public class AdminShelfController {

    private final AdminShelfService adminShelfService;

    public AdminShelfController(AdminShelfService adminShelfService) {
        this.adminShelfService = adminShelfService;
    }

    @GetMapping
    public ApiResponse<PageResult<ShelfApplicationAdminItem>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminShelfService.list(status, storeId, merchantOrgId, page, pageSize));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ShelfApplicationAdminItem> approve(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(adminShelfService.approve(AdminPrincipal.adminId(authentication), id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ShelfApplicationAdminItem> reject(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectReasonRequest request) {
        return ApiResponse.ok(adminShelfService.reject(AdminPrincipal.adminId(authentication), id, request));
    }
}
