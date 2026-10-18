package com.suzhong.njl.web.admin;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.service.admin.AdminBannerService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.BannerAdminResponse;
import com.suzhong.njl.web.admin.dto.BannerWriteRequest;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    public AdminBannerController(AdminBannerService adminBannerService) {
        this.adminBannerService = adminBannerService;
    }

    @GetMapping
    public ApiResponse<PageResult<BannerAdminResponse>> list(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) BannerScope scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminBannerService.list(enabled, scope, keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<BannerAdminResponse> create(
            Authentication authentication,
            @RequestBody BannerWriteRequest request) {
        return ApiResponse.ok(adminBannerService.create(AdminPrincipal.adminId(authentication), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<BannerAdminResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminBannerService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<BannerAdminResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody BannerWriteRequest request) {
        return ApiResponse.ok(adminBannerService.update(AdminPrincipal.adminId(authentication), id, request));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<BannerAdminResponse> enable(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(adminBannerService.setEnabled(AdminPrincipal.adminId(authentication), id, true));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<BannerAdminResponse> disable(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(adminBannerService.setEnabled(AdminPrincipal.adminId(authentication), id, false));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Long>> delete(Authentication authentication, @PathVariable Long id) {
        adminBannerService.delete(AdminPrincipal.adminId(authentication), id);
        return ApiResponse.ok(Map.of("id", id));
    }
}
