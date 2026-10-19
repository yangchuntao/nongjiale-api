package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ArticleStatus;

/**
 * 运营端攻略详情（{@code GET /api/admin/articles} 的 items 元素、
 * {@code GET /api/admin/articles/{id}}、新建与全量更新的 data）。
 *
 * <p>后台列表包含草稿，正文也直接返回以便编辑器回填；门户只暴露 PUBLISHED 且不含草稿。
 * 时间字段为 {@code Asia/Shanghai} 的 ISO-8601。
 *
 * @param id          攻略 ID，对应 {@code article.id}
 * @param title       标题，非空，最长 200 字
 * @param coverUrl    封面图 URL，可空，最长 512
 * @param summary     摘要，可空，最长 500
 * @param body        正文文本，可空；门户按原文本输出，不做富文本清洗
 * @param status      发布状态，取值见 {@link ArticleStatus}：DRAFT（草稿）、PUBLISHED（已发布）
 * @param regionId    所属地区 ID，可空；用于门户按市聚合
 * @param publishedAt 首次发布时间，可空；仅在状态首次变为 PUBLISHED 时写入，退回草稿后保留原值
 * @param createdBy   创建人运营 ID
 * @param createdAt   创建时间
 * @param updatedAt   最后更新时间
 */
public record ArticleAdminResponse(
        Long id,
        String title,
        String coverUrl,
        String summary,
        String body,
        ArticleStatus status,
        Long regionId,
        LocalDateTime publishedAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
