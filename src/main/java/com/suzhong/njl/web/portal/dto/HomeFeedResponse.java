package com.suzhong.njl.web.portal.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 首页聚合内容（{@code GET /api/home/feed} 的响应体，公开接口）。
 *
 * <p>把推荐门店与最新攻略合成一个响应，减少首屏请求数。门店要求 {@code homeRecommended=true} 且已上架，
 * 并额外要求所属市已开通；攻略只取已发布且可主推的，最多 6 条。整份结果走 Redis 旁路缓存，
 * 开城/关城与运营改推荐位后需失效缓存。
 *
 * @param stores   推荐门店卡片，按首页排序号升序、ID 升序；无数据时为空列表而不是 null
 * @param articles 最新攻略摘要，最多 6 条；无数据时为空列表而不是 null
 */
@Schema(name = "HomeFeedResponse", description = "首页聚合内容，GET /api/home/feed 的响应体；合并推荐门店与最新攻略，结果走 Redis 旁路缓存")
public record HomeFeedResponse(
        @Schema(description = "推荐门店卡片，按首页排序号升序、ID 升序；无数据时为空列表而不是 null")
        List<StoreCard> stores,
        @Schema(description = "最新攻略摘要，最多 6 条；无数据时为空列表而不是 null")
        List<ArticleSummary> articles) {
}
