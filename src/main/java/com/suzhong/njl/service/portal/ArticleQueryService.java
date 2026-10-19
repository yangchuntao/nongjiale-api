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

/**
 * 攻略查询服务（门户端）。
 *
 * <p>职责：提供攻略分页列表、首页最新攻略与攻略详情。列表与首页共用同一套「可主推」过滤：
 * 已发布，且地区为空、挂省、或所属市仍开通；已关闭市的攻略不占列表与首页名额。
 *
 * <p>关键区别：过滤只作用于<b>列表/推荐</b>；已发布攻略的直链详情不因关城变成 404，
 * 这是开城门禁要求的「正文可打开，不主推」。
 *
 * <p>无缓存、无事务，过滤条件全部下推到 SQL；详情只读单表。
 */
@Service
public class ArticleQueryService {

    private final ArticleMapper articleMapper;
    private final CityVisibility cityVisibility;

    public ArticleQueryService(ArticleMapper articleMapper, CityVisibility cityVisibility) {
        this.articleMapper = articleMapper;
        this.cityVisibility = cityVisibility;
    }

    /**
     * 分页查询门户可见攻略。
     *
     * <p>流程：规范化分页 → 先 count → 取当页（发布时间倒序，同时间按 ID 倒序）→ 转摘要。
     *
     * @param page     页码，从 1 开始，为空取默认值
     * @param pageSize 每页条数，为空取默认值，上限 100
     * @return 当页摘要 + 总数 + 分页信息
     * @throws ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<ArticleSummary> list(Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        // 先取总数：与明细复用同一个可见性条件，保证 total 与 items 口径一致
        Long total = articleMapper.selectCount(visibleFilter());
        // LIMIT/OFFSET 直接拼字符串，取值来自已校验的 int，不存在注入面
        List<Article> rows = articleMapper.selectList(ordered(visibleFilter())
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        return new PageResult<>(toSummaries(rows), total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 取最新可主推攻略，供首页 feed 使用。
     *
     * <p>先按可见性过滤再 {@code LIMIT}，关闭市攻略不会挤掉其他市的名额；结果不单独缓存，
     * 由首页 feed 的整体缓存承担。
     *
     * @param limit 条数上限，由调用方保证为正数（首页固定 6）
     * @return 摘要列表，可能为空
     */
    public List<ArticleSummary> latestPromoted(int limit) {
        List<Article> rows = articleMapper.selectList(ordered(visibleFilter()).last("LIMIT " + limit));
        return toSummaries(rows);
    }

    /**
     * 查询攻略详情。
     *
     * <p>只校验存在且已发布，不校验所属城市是否开通：直链保持可读，草稿仍返回 404。
     *
     * @param id 攻略 ID
     * @return 攻略正文与元信息
     * @throws ApiException 不存在或未发布时 404「攻略不存在」
     */
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

    /**
     * 构造「可主推」过滤条件：已发布，且地区为空或落在可露出地区内。
     *
     * <p>地区集合来自 {@link CityVisibility#promotedArticleRegionIds()}；为空时仍保留 {@code region_id IS NULL}
     * 分支，不能省掉整个嵌套条件，否则会退化成全表。
     */
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

    /** 统一排序：发布时间倒序，同一时间用 ID 倒序兜底，保证分页稳定。 */
    private LambdaQueryWrapper<Article> ordered(LambdaQueryWrapper<Article> query) {
        return query.orderByDesc(Article::getPublishedAt).orderByDesc(Article::getId);
    }

    /** 批量转摘要，列表与首页共用。 */
    private List<ArticleSummary> toSummaries(List<Article> rows) {
        List<ArticleSummary> items = new ArrayList<>(rows.size());
        for (Article row : rows) {
            items.add(toSummary(row));
        }
        return items;
    }

    /** 单条转摘要，不返回正文，避免列表响应过大。 */
    private ArticleSummary toSummary(Article article) {
        return new ArticleSummary(
                article.getId(),
                article.getTitle(),
                article.getCoverUrl(),
                article.getSummary(),
                article.getPublishedAt());
    }
}
