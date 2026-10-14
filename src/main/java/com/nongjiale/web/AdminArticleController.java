package com.nongjiale.web;

import com.nongjiale.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/articles")
@PreAuthorize("hasRole('OPERATOR')")
public class AdminArticleController {
    private final ArticleService articles;

    public AdminArticleController(ArticleService articles) {
        this.articles = articles;
    }

    public record ArticleReq(String title, String coverUrl, String summary, String body) {}
    public record StatusReq(String status) {}

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<Map<String, Object>> result = articles.adminList(status, page, pageSize);
        return ApiResponse.ok(new PageResult<>(result.getContent(), result.getTotalElements()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody ArticleReq req) {
        ArticleReq body = req == null ? new ArticleReq(null, null, null, null) : req;
        return ApiResponse.ok(articles.create(body.title(), body.coverUrl(), body.summary(), body.body()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody ArticleReq req) {
        ArticleReq body = req == null ? new ArticleReq(null, null, null, null) : req;
        return ApiResponse.ok(articles.update(id, body.title(), body.coverUrl(), body.summary(), body.body()));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> status(@PathVariable Long id, @RequestBody StatusReq req) {
        return ApiResponse.ok(articles.updateStatus(id, req == null ? null : req.status()));
    }
}
