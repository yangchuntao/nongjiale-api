package com.suzhong.njl.service.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.web.portal.dto.ArticleSummary;
import com.suzhong.njl.web.portal.dto.HomeFeedResponse;
import com.suzhong.njl.web.portal.dto.RegionItem;
import com.suzhong.njl.web.portal.dto.StoreCard;

@Service
public class HomeFeedService {

    private static final int ARTICLE_LIMIT = 6;
    private static final TypeReference<HomeFeedResponse> TYPE = new TypeReference<>() {
    };

    private final StoreMapper storeMapper;
    private final ArticleMapper articleMapper;
    private final RegionQueryService regionQueryService;
    private final StoreQueryService storeQueryService;
    private final CacheAside cacheAside;

    public HomeFeedService(
            StoreMapper storeMapper,
            ArticleMapper articleMapper,
            RegionQueryService regionQueryService,
            StoreQueryService storeQueryService,
            CacheAside cacheAside) {
        this.storeMapper = storeMapper;
        this.articleMapper = articleMapper;
        this.regionQueryService = regionQueryService;
        this.storeQueryService = storeQueryService;
        this.cacheAside = cacheAside;
    }

    public HomeFeedResponse feed() {
        return cacheAside.get(CacheName.HOME_FEED, "default", TYPE, this::load);
    }

    private HomeFeedResponse load() {
        List<Store> stores = storeMapper.selectList(
                Wrappers.<Store>lambdaQuery()
                        .eq(Store::getShelfStatus, ShelfStatus.ON_SHELF)
                        .eq(Store::getHomeRecommended, true)
                        .orderByAsc(Store::getHomeSort)
                        .orderByAsc(Store::getId));
        Map<Long, String> regionNames = new HashMap<>();
        for (RegionItem region : regionQueryService.enabledRegions()) {
            regionNames.put(region.id(), region.name());
        }
        List<StoreCard> cards = new ArrayList<>(stores.size());
        for (Store store : stores) {
            cards.add(storeQueryService.toCard(store, regionNames));
        }
        List<Article> articles = articleMapper.selectList(
                Wrappers.<Article>lambdaQuery()
                        .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                        .orderByDesc(Article::getPublishedAt)
                        .orderByDesc(Article::getId)
                        .last("LIMIT " + ARTICLE_LIMIT));
        List<ArticleSummary> summaries = new ArrayList<>(articles.size());
        for (Article article : articles) {
            summaries.add(new ArticleSummary(
                    article.getId(),
                    article.getTitle(),
                    article.getCoverUrl(),
                    article.getSummary(),
                    article.getPublishedAt()));
        }
        return new HomeFeedResponse(cards, summaries);
    }
}
