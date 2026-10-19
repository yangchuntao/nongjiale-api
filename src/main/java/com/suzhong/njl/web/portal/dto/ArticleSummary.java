package com.suzhong.njl.web.portal.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 攻略摘要（{@code GET /api/articles} 的 items 元素，以及 {@code GET /api/home/feed} 的 articles 元素）。
 *
 * <p>只含列表展示字段，不含正文，避免列表响应过大。列表与首页都只取已发布且可主推的攻略：
 * 已关闭市的攻略不占名额，但已发布攻略的直链详情仍可打开。
 *
 * @param id          攻略 ID，用于跳转详情
 * @param title       标题
 * @param coverUrl    封面图 URL，可空
 * @param summary     摘要/导语，可空
 * @param publishedAt 发布时间，可空；列表按该字段倒序，同时间用 ID 倒序兜底
 */
@Schema(name = "ArticleSummary", description = "攻略摘要，GET /api/articles 的 items 元素与 GET /api/home/feed 的 articles 元素；只含列表展示字段，不含正文")
public record ArticleSummary(
        @Schema(description = "攻略 ID，用于跳转详情")
        Long id,
        @Schema(description = "标题", example = "苏中周末住哪里")
        String title,
        @Schema(description = "封面图 URL，可空")
        String coverUrl,
        @Schema(description = "摘要/导语，可空")
        String summary,
        @Schema(description = "发布时间，可空；列表按该字段倒序，同时间用 ID 倒序兜底")
        LocalDateTime publishedAt) {
}
