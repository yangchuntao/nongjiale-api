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

import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.service.admin.AdminArticleService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ArticleAdminResponse;
import com.suzhong.njl.web.admin.dto.ArticleWriteRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final AdminArticleService adminArticleService;

    public AdminArticleController(AdminArticleService adminArticleService) {
        this.adminArticleService = adminArticleService;
    }

    @GetMapping
    public ApiResponse<PageResult<ArticleAdminResponse>> list(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminArticleService.list(status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ArticleAdminResponse> create(
            Authentication authentication,
            @Valid @RequestBody ArticleWriteRequest request) {
        return ApiResponse.ok(adminArticleService.create(AdminPrincipal.adminId(authentication), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleAdminResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminArticleService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleWriteRequest request) {
        return ApiResponse.ok(adminArticleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Long>> delete(@PathVariable Long id) {
        adminArticleService.delete(id);
        return ApiResponse.ok(Map.of("id", id));
    }
}
