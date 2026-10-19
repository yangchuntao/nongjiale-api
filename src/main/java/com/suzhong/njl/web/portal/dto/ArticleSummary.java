package com.suzhong.njl.web.portal.dto;

import java.time.LocalDateTime;

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
public record ArticleSummary(
        Long id,
        String title,
        String coverUrl,
        String summary,
        LocalDateTime publishedAt) {
}
