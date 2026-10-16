package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.admin.AdminSiteConfigService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.SiteConfigResponse;
import com.suzhong.njl.web.admin.dto.SiteConfigWriteRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/site-config")
public class AdminSiteConfigController {

    private final AdminSiteConfigService adminSiteConfigService;

    public AdminSiteConfigController(AdminSiteConfigService adminSiteConfigService) {
        this.adminSiteConfigService = adminSiteConfigService;
    }

    @GetMapping
    public ApiResponse<SiteConfigResponse> get() {
        return ApiResponse.ok(adminSiteConfigService.get());
    }

    @PutMapping
    public ApiResponse<SiteConfigResponse> update(
            Authentication authentication,
            @Valid @RequestBody SiteConfigWriteRequest request) {
        return ApiResponse.ok(adminSiteConfigService.update(AdminPrincipal.adminId(authentication), request));
    }
}
