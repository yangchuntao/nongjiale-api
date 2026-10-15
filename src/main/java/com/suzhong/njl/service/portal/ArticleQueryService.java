package com.suzhong.njl.service.portal;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.ArticleDetailResponse;
import com.suzhong.njl.web.portal.dto.ArticleSummary;

@Service
public class ArticleQueryService {

    private final ArticleMapper articleMapper;

    public ArticleQueryService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public PageResult<ArticleSummary> list(Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = articleMapper.selectCount(published());
        List<Article> rows = articleMapper.selectList(
                published().last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<ArticleSummary> items = new ArrayList<>(rows.size());
        for (Article row : rows) {
            items.add(toSummary(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public ArticleDetailResponse detail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "攻略不存在");
        }
        return new ArticleDetailResponse(
                article.getId(),
                article.getTitle(),
                article.getCoverUrl(),
                article.getSummary(),
                article.getBody(),
                article.getStatus(),
                article.getRegionId(),
                article.getPublishedAt());
    }

    private LambdaQueryWrapper<Article> published() {
        return Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .orderByDesc(Article::getPublishedAt)
                .orderByDesc(Article::getId);
    }

    private ArticleSummary toSummary(Article article) {
        return new ArticleSummary(
                article.getId(),
                article.getTitle(),
                article.getCoverUrl(),
                article.getSummary(),
                article.getPublishedAt());
    }
}
