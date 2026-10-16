package com.suzhong.njl.service.admin;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ArticleAdminResponse;
import com.suzhong.njl.web.admin.dto.ArticleWriteRequest;

@Service
public class AdminArticleService {

    private final ArticleMapper articleMapper;
    private final RegionMapper regionMapper;
    private final AdminCache adminCache;
    private final Clock applicationClock;

    public AdminArticleService(
            ArticleMapper articleMapper,
            RegionMapper regionMapper,
            AdminCache adminCache,
            Clock applicationClock) {
        this.articleMapper = articleMapper;
        this.regionMapper = regionMapper;
        this.adminCache = adminCache;
        this.applicationClock = applicationClock;
    }

    public PageResult<ArticleAdminResponse> list(ArticleStatus status, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = articleMapper.selectCount(filter(status));
        List<Article> rows = articleMapper.selectList(filter(status)
                .orderByDesc(Article::getUpdatedAt)
                .orderByDesc(Article::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<ArticleAdminResponse> items = new ArrayList<>(rows.size());
        for (Article row : rows) {
            items.add(toResponse(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public ArticleAdminResponse detail(Long id) {
        return toResponse(requireArticle(id));
    }

    @Transactional
    public ArticleAdminResponse create(long adminId, ArticleWriteRequest request) {
        requireRegion(request.regionId());
        Article article = new Article();
        article.setTitle(request.title());
        article.setCoverUrl(request.coverUrl());
        article.setSummary(request.summary());
        article.setBody(request.body());
        article.setStatus(request.status());
        article.setRegionId(request.regionId());
        article.setCreatedBy(adminId);
        if (request.status() == ArticleStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now(applicationClock));
        }
        articleMapper.insert(article);
        adminCache.evictArticles();
        return toResponse(requireArticle(article.getId()));
    }

    @Transactional
    public ArticleAdminResponse update(Long id, ArticleWriteRequest request) {
        Article article = requireArticle(id);
        requireRegion(request.regionId());
        ArticleStatus previous = article.getStatus();
        article.setTitle(request.title());
        article.setCoverUrl(request.coverUrl());
        article.setSummary(request.summary());
        article.setBody(request.body());
        article.setStatus(request.status());
        article.setRegionId(request.regionId());
        if (request.status() == ArticleStatus.PUBLISHED && previous != ArticleStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now(applicationClock));
        }
        articleMapper.updateById(article);
        adminCache.evictArticles();
        return toResponse(requireArticle(id));
    }

    @Transactional
    public void delete(Long id) {
        if (articleMapper.deleteById(id) != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "攻略不存在");
        }
        adminCache.evictArticles();
    }

    private LambdaQueryWrapper<Article> filter(ArticleStatus status) {
        LambdaQueryWrapper<Article> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(Article::getStatus, status);
        }
        return query;
    }

    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "攻略不存在");
        }
        return article;
    }

    private void requireRegion(Long regionId) {
        if (regionId == null) {
            return;
        }
        Region region = regionMapper.selectById(regionId);
        if (region == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不存在");
        }
    }

    private ArticleAdminResponse toResponse(Article article) {
        return new ArticleAdminResponse(
                article.getId(),
                article.getTitle(),
                article.getCoverUrl(),
                article.getSummary(),
                article.getBody(),
                article.getStatus(),
                article.getRegionId(),
                article.getPublishedAt(),
                article.getCreatedBy(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }
}
