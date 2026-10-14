package com.nongjiale.service;

import com.nongjiale.domain.Article;
import com.nongjiale.repository.ArticleRepository;
import com.nongjiale.web.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ArticleService {
    private final ArticleRepository articles;

    public ArticleService(ArticleRepository articles) {
        this.articles = articles;
    }

    public Page<Map<String, Object>> published(int page, int pageSize) {
        return articles.findByStatusOrderByPublishedAtDesc(
                "PUBLISHED", PageRequests.of(page, pageSize)).map(a -> toDto(a, true));
    }

    public Map<String, Object> publishedDetail(Long id) {
        Article article = articles.findById(id)
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "资讯不存在"));
        return toDto(article, true);
    }

    public Page<Map<String, Object>> adminList(String status, int page, int pageSize) {
        String st = PageRequests.blankToNull(status);
        Page<Article> result = st == null
                ? articles.findAllByOrderByCreatedAtDesc(PageRequests.of(page, pageSize))
                : articles.findByStatusOrderByCreatedAtDesc(st, PageRequests.of(page, pageSize));
        return result.map(a -> toDto(a, true));
    }

    @Transactional
    public Map<String, Object> create(String title, String coverUrl, String summary, String body) {
        Article article = new Article();
        article.setTitle(requireTitle(title));
        article.setCoverUrl(coverUrl);
        article.setSummary(summary);
        article.setBody(body);
        article.setStatus("DRAFT");
        return toDto(articles.save(article), true);
    }

    @Transactional
    public Map<String, Object> update(Long id, String title, String coverUrl, String summary, String body) {
        Article article = require(id);
        if (title != null) article.setTitle(requireTitle(title));
        if (coverUrl != null) article.setCoverUrl(coverUrl);
        if (summary != null) article.setSummary(summary);
        if (body != null) article.setBody(body);
        return toDto(article, true);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status) {
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "非法状态");
        }
        Article article = require(id);
        article.setStatus(status);
        if ("PUBLISHED".equals(status) && article.getPublishedAt() == null) {
            article.setPublishedAt(Instant.now());
        }
        return toDto(article, true);
    }

    public Map<String, Object> card(Article article) {
        return toDto(article, false);
    }

    private Article require(Long id) {
        return articles.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "资讯不存在"));
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "标题不能为空");
        }
        return title.trim();
    }

    private Map<String, Object> toDto(Article article, boolean includeBody) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", article.getId());
        m.put("title", article.getTitle());
        m.put("coverUrl", article.getCoverUrl());
        m.put("summary", article.getSummary() == null ? "" : article.getSummary());
        if (includeBody) {
            m.put("body", article.getBody() == null ? "" : article.getBody());
        }
        m.put("status", article.getStatus());
        m.put("publishedAt", article.getPublishedAt() == null ? null : article.getPublishedAt().toString());
        m.put("createdAt", article.getCreatedAt().toString());
        return m;
    }
}
