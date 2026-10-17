package com.suzhong.njl.service.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.service.city.CityVisibility;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.portal.dto.ArticleDetailResponse;
import com.suzhong.njl.web.portal.dto.ArticleSummary;

@Service
public class ArticleQueryService {

    private final ArticleMapper articleMapper;
    private final CityVisibility cityVisibility;

    public ArticleQueryService(ArticleMapper articleMapper, CityVisibility cityVisibility) {
        this.articleMapper = articleMapper;
        this.cityVisibility = cityVisibility;
    }

    public PageResult<ArticleSummary> list(Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = articleMapper.selectCount(visibleFilter());
        List<Article> rows = articleMapper.selectList(ordered(visibleFilter())
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        return new PageResult<>(toSummaries(rows), total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public List<ArticleSummary> latestPromoted(int limit) {
        List<Article> rows = articleMapper.selectList(ordered(visibleFilter()).last("LIMIT " + limit));
        return toSummaries(rows);
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

    private LambdaQueryWrapper<Article> visibleFilter() {
        Set<Long> regionIds = cityVisibility.promotedArticleRegionIds();
        LambdaQueryWrapper<Article> query = Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED);
        query.and(nested -> {
            nested.isNull(Article::getRegionId);
            if (!regionIds.isEmpty()) {
                nested.or().in(Article::getRegionId, regionIds);
            }
        });
        return query;
    }

    private LambdaQueryWrapper<Article> ordered(LambdaQueryWrapper<Article> query) {
        return query.orderByDesc(Article::getPublishedAt).orderByDesc(Article::getId);
    }

    private List<ArticleSummary> toSummaries(List<Article> rows) {
        List<ArticleSummary> items = new ArrayList<>(rows.size());
        for (Article row : rows) {
            items.add(toSummary(row));
        }
        return items;
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
