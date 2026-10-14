package com.nongjiale.web;

import com.nongjiale.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class PortalArticleController {
    private final ArticleService articles;

    public PortalArticleController(ArticleService articles) {
        this.articles = articles;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<Map<String, Object>> result = articles.published(page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(articles.publishedDetail(id));
    }
}
