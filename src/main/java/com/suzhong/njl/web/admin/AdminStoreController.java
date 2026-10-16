package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.service.admin.AdminStoreService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminStoreResponse;
import com.suzhong.njl.web.admin.dto.AdminStoreUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    public AdminStoreController(AdminStoreService adminStoreService) {
        this.adminStoreService = adminStoreService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminStoreResponse>> list(
            @RequestParam(required = false) ShelfStatus shelfStatus,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long merchantOrgId,
            @RequestParam(required = false) Boolean homeRecommended,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminStoreService.list(
                shelfStatus, regionId, merchantOrgId, homeRecommended, name, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminStoreResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminStoreService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminStoreResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminStoreUpdateRequest request) {
        return ApiResponse.ok(adminStoreService.update(AdminPrincipal.adminId(authentication), id, request));
    }
}
