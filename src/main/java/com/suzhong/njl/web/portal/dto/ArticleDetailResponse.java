package com.suzhong.njl.web.portal.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ArticleStatus;

/**
 * 攻略详情（{@code GET /api/articles/{id}} 的响应体，位于统一信封的 {@code data} 中）。
 *
 * <p>门户只返回已发布攻略：草稿与不存在的 ID 一律 404「攻略不存在」。已发布攻略即使所属城市后来关城，
 * 直链仍可打开，因此本结构不携带城市开通态。
 *
 * @param id          攻略 ID
 * @param title       标题
 * @param coverUrl    封面图 URL，可空；为空时前端按无封面处理
 * @param summary     摘要/导语，可空
 * @param body        正文内容，可空
 * @param status      发布状态，见 {@link com.suzhong.njl.domain.enums.ArticleStatus}；门户详情实际只会是 {@code PUBLISHED}
 * @param regionId    归属地区 ID，可空；为空表示不限城市的通用攻略
 * @param publishedAt 发布时间，可空；未回填时前端按未知时间展示
 */
public record ArticleDetailResponse(
        Long id,
        String title,
        String coverUrl,
        String summary,
        String body,
        ArticleStatus status,
        Long regionId,
        LocalDateTime publishedAt) {
}
