package com.suzhong.njl.web.admin;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.admin.AdminCityService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.admin.dto.ChecklistPatchRequest;
import com.suzhong.njl.web.admin.dto.CityDetailResponse;
import com.suzhong.njl.web.admin.dto.CitySummaryResponse;
import com.suzhong.njl.web.admin.dto.CityVersionRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/cities")
public class AdminCityController {

    private final AdminCityService adminCityService;

    public AdminCityController(AdminCityService adminCityService) {
        this.adminCityService = adminCityService;
    }

    @GetMapping
    public ApiResponse<List<CitySummaryResponse>> list() {
        return ApiResponse.ok(adminCityService.list());
    }

    @GetMapping("/{cityId}")
    public ApiResponse<CityDetailResponse> detail(@PathVariable Long cityId) {
        return ApiResponse.ok(adminCityService.detail(cityId));
    }

    @PatchMapping("/{cityId}/checklist")
    public ApiResponse<CityDetailResponse> patch(
            Authentication authentication,
            @PathVariable Long cityId,
            @Valid @RequestBody ChecklistPatchRequest request) {
        return ApiResponse.ok(adminCityService.patch(AdminPrincipal.adminId(authentication), cityId, request));
    }

    @PostMapping("/{cityId}/open")
    public ApiResponse<CityDetailResponse> open(
            Authentication authentication,
            @PathVariable Long cityId,
            @Valid @RequestBody CityVersionRequest request) {
        return ApiResponse.ok(adminCityService.open(AdminPrincipal.adminId(authentication), cityId, request.version()));
    }

    @PostMapping("/{cityId}/close")
    public ApiResponse<CityDetailResponse> close(
            Authentication authentication,
            @PathVariable Long cityId,
            @Valid @RequestBody CityVersionRequest request) {
        return ApiResponse.ok(adminCityService.close(AdminPrincipal.adminId(authentication), cityId, request.version()));
    }
}
