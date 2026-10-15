package com.suzhong.njl.web.portal.dto;

import java.util.List;

public record HomeFeedResponse(
        List<StoreCard> stores,
        List<ArticleSummary> articles) {
}
