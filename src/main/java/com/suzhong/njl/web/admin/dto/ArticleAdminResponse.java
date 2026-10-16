package com.suzhong.njl.web.admin.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ArticleStatus;

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
