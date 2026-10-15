package com.suzhong.njl.web.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.service.portal.ArticleQueryService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.ArticleDetailResponse;
import com.suzhong.njl.web.portal.dto.ArticleSummary;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleQueryService articleQueryService;

    public ArticleController(ArticleQueryService articleQueryService) {
        this.articleQueryService = articleQueryService;
    }

    @GetMapping
    public ApiResponse<PageResult<ArticleSummary>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(articleQueryService.list(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(articleQueryService.detail(id));
    }
}
