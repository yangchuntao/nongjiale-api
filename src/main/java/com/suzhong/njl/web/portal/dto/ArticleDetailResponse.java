package com.suzhong.njl.web.portal.dto;

import java.time.LocalDateTime;

import com.suzhong.njl.domain.enums.ArticleStatus;

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
