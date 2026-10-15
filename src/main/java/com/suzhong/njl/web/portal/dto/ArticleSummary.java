package com.suzhong.njl.web.portal.dto;

import java.time.LocalDateTime;

public record ArticleSummary(
        Long id,
        String title,
        String coverUrl,
        String summary,
        LocalDateTime publishedAt) {
}
