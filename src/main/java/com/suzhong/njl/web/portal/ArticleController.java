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

/**
 * 门户攻略接口（{@code /api/articles}）。
 *
 * <p>所属端：门户游客，公开接口，无需 Bearer。
 *
 * <p>职责：已发布攻略的分页列表与详情。列表只返回已发布且可主推的攻略（地区为空、挂省，
 * 或所属市仍开通）；已发布攻略的直链详情不因关城失效，草稿始终 404。
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleQueryService articleQueryService;

    public ArticleController(ArticleQueryService articleQueryService) {
        this.articleQueryService = articleQueryService;
    }

    /**
     * {@code GET /api/articles} — 已发布攻略列表（公开）。
     *
     * <p>按发布时间倒序、ID 倒序；已关闭市的攻略不占列表名额，地区为空的通用攻略始终展示。
     *
     * @param page     页码，从 1 开始，为空取 1
     * @param pageSize 每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页攻略摘要（不含正文）与总数
     */
    @GetMapping
    public ApiResponse<PageResult<ArticleSummary>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(articleQueryService.list(page, pageSize));
    }

    /**
     * {@code GET /api/articles/{id}} — 攻略详情（公开）。
     *
     * <p>只校验存在且已发布，不校验所属城市是否开通：分享出去的直链在关城后仍可打开。
     *
     * @param id 攻略 ID
     * @return 攻略正文、封面、摘要、状态、地区与发布时间
     * @throws com.suzhong.njl.web.ApiException 攻略不存在或仍是草稿 404「攻略不存在」
     */
    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(articleQueryService.detail(id));
    }
}
