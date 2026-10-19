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

/**
 * 运营端攻略管理服务（{@code /api/admin/articles}）。
 *
 * <p>职责：攻略的列表、详情、新建、全量更新与物理删除。发布态由 {@link ArticleStatus} 控制，
 * 只有 {@code PUBLISHED} 才对门户列表可见，草稿只在运营端出现。
 *
 * <p>事务边界：写操作（{@code create}/{@code update}/{@code delete}）各自一个事务，
 * 数据落库与缓存失效在同一调用内完成；读操作不开事务。
 *
 * <p>权限：本类不做角色校验，{@code /api/admin/**} 的 Bearer 与 {@code OPERATOR}/{@code AGENT}
 * 角色由安全配置与控制器入口保证。
 *
 * <p>依赖：{@link AdminCache} 负责在写成功后失效首页聚合缓存（{@code HOME_FEED}）；
 * 删除失败只记日志，不回滚已提交的攻略写操作。
 */
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

    /**
     * 分页查询攻略，含草稿。
     *
     * <p>流程：规范化分页 → 按状态组装过滤条件 → 先 count 再取当页 → 逐行转响应。
     * 排序固定为最近更新优先，同一时间再按 id 倒序，保证翻页稳定。
     *
     * @param status   状态筛选，{@code null} 表示草稿与已发布都要
     * @param page     页码，从 1 开始，为空取默认值
     * @param pageSize 每页条数，为空取默认值，上限 100
     * @return 当页攻略与总数
     * @throws com.suzhong.njl.web.ApiException 分页参数非法时 400「分页参数不正确」
     */
    public PageResult<ArticleAdminResponse> list(ArticleStatus status, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        // 先 count：与列表复用同一套过滤条件，避免总数与当页口径不一致
        Long total = articleMapper.selectCount(filter(status));
        List<Article> rows = articleMapper.selectList(filter(status)
                // 分页下推到 SQL，避免把全部攻略捞回内存；pageSize 已由 PageQuery 限制在 100 以内
                .orderByDesc(Article::getUpdatedAt)
                .orderByDesc(Article::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<ArticleAdminResponse> items = new ArrayList<>(rows.size());
        for (Article row : rows) {
            items.add(toResponse(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    /**
     * 查询攻略详情（含草稿）。
     *
     * @param id 攻略 ID
     * @return 攻略完整字段
     * @throws com.suzhong.njl.web.ApiException 攻略不存在时 404「攻略不存在」
     */
    public ArticleAdminResponse detail(Long id) {
        return toResponse(requireArticle(id));
    }

    /**
     * 新建攻略。
     *
     * <p>流程：校验地区（可空）→ 组装实体（创建人取当前运营）→ 若直接以已发布状态创建则写入发布时间
     * → 落库 → 失效首页缓存 → 回读返回。
     *
     * @param adminId 当前运营账号 ID，写入 {@code created_by}
     * @param request 攻略写入参数，标题、状态由 DTO 校验
     * @return 新建后的攻略详情
     * @throws com.suzhong.njl.web.ApiException 地区 ID 存在但查不到时 400「地区不存在」
     */
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
        // 以已发布状态创建时立即写入发布时间；草稿的该字段保持为空
        if (request.status() == ArticleStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now(applicationClock));
        }
        articleMapper.insert(article);
        adminCache.evictArticles();
        return toResponse(requireArticle(article.getId()));
    }

    /**
     * 全量更新攻略。
     *
     * <p>流程：读取原记录 → 校验地区 → 覆盖可写字段 → 仅在「由非发布转为发布」时补发布时间
     * → 乐观锁更新（版本来自实体）→ 失效首页缓存 → 回读返回。
     *
     * @param id      攻略 ID
     * @param request 攻略写入参数
     * @return 更新后的攻略详情
     * @throws com.suzhong.njl.web.ApiException 攻略不存在时 404「攻略不存在」；地区不存在时 400「地区不存在」
     */
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
        // 只在首次发布时补发布时间；已发布继续编辑不清空原发布时间，撤回草稿再发布则重新计时
        if (request.status() == ArticleStatus.PUBLISHED && previous != ArticleStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now(applicationClock));
        }
        articleMapper.updateById(article);
        adminCache.evictArticles();
        return toResponse(requireArticle(id));
    }

    /**
     * 物理删除攻略。
     *
     * <p>删除成功后才失效首页缓存；影响行数不为 1 说明记录已不存在。
     *
     * @param id 攻略 ID
     * @throws com.suzhong.njl.web.ApiException 攻略不存在时 404「攻略不存在」
     */
    @Transactional
    public void delete(Long id) {
        if (articleMapper.deleteById(id) != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "攻略不存在");
        }
        adminCache.evictArticles();
    }

    /** 组装攻略列表过滤条件：状态为空表示不筛。 */
    private LambdaQueryWrapper<Article> filter(ArticleStatus status) {
        LambdaQueryWrapper<Article> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(Article::getStatus, status);
        }
        return query;
    }

    /** 按 ID 取攻略，不存在直接抛 404，供详情与写操作复用。 */
    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "攻略不存在");
        }
        return article;
    }

    /**
     * 校验攻略所属地区：允许为空（不挂地区），非空时必须能在库中查到。
     * 这里只校验存在性，不要求地区启用，也不要求所属市已开通。
     */
    private void requireRegion(Long regionId) {
        if (regionId == null) {
            return;
        }
        Region region = regionMapper.selectById(regionId);
        if (region == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不存在");
        }
    }

    /** 实体转运营端响应，字段与门户 DTO 解耦，草稿也完整返回正文。 */
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
