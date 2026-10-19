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
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.city.CityVisibility;
import com.suzhong.njl.web.portal.dto.ArticleSummary;
import com.suzhong.njl.web.portal.dto.HomeFeedResponse;
import com.suzhong.njl.web.portal.dto.RegionItem;
import com.suzhong.njl.web.portal.dto.StoreCard;

/**
 * 首页聚合服务（门户端）。
 *
 * <p>职责：把首页推荐门店与最新攻略合成一个响应，减少门户首屏请求数。
 *
 * <p>可见性：推荐店要求 {@code home_recommended=true} 且已上架，并额外要求所属市已开通；
 * 攻略复用 {@link ArticleQueryService#latestPromoted}，已关闭市的攻略不占名额。
 *
 * <p>缓存：整份 feed 走 Redis 旁路缓存，键 {@code home-feed:default}，TTL 见
 * {@code app.cache.home-feed-ttl}；开城/关城与运营改推荐位后必须删这把键。
 *
 * <p>只读，无事务；缓存不可用时直接读库。
 */
@Service
public class HomeFeedService {

    /** 首页攻略条数上限；SQL 先过滤再 LIMIT，关闭市的攻略不会挤占名额。 */
    private static final int ARTICLE_LIMIT = 6;
    private static final TypeReference<HomeFeedResponse> TYPE = new TypeReference<>() {
    };

    private final StoreMapper storeMapper;
    private final RegionQueryService regionQueryService;
    private final StoreQueryService storeQueryService;
    private final ArticleQueryService articleQueryService;
    private final CityVisibility cityVisibility;
    private final CacheAside cacheAside;

    public HomeFeedService(
            StoreMapper storeMapper,
            RegionQueryService regionQueryService,
            StoreQueryService storeQueryService,
            ArticleQueryService articleQueryService,
            CityVisibility cityVisibility,
            CacheAside cacheAside) {
        this.storeMapper = storeMapper;
        this.regionQueryService = regionQueryService;
        this.storeQueryService = storeQueryService;
        this.articleQueryService = articleQueryService;
        this.cityVisibility = cityVisibility;
        this.cacheAside = cacheAside;
    }

    /**
     * 查询首页聚合内容。
     *
     * @return 推荐门店卡片 + 最新攻略摘要；无数据时两个列表均为空
     */
    public HomeFeedResponse feed() {
        return cacheAside.get(CacheName.HOME_FEED, "default", TYPE, this::load);
    }

    /** 加载首页数据：推荐的已上架门店按首页排序，再在内存剔除未开通城市，最后取最新攻略。 */
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
        Map<Long, Region> regions = cityVisibility.byId();
        List<StoreCard> cards = new ArrayList<>();
        for (Store store : stores) {
            // 城市开通过滤留在内存：推荐位数量很少，且要复用已加载的 region 索引
            if (!cityVisibility.isCustomerStoreRegion(store.getRegionId(), regions)) {
                continue;
            }
            cards.add(storeQueryService.toCard(store, regionNames));
        }
        List<ArticleSummary> summaries = articleQueryService.latestPromoted(ARTICLE_LIMIT);
        return new HomeFeedResponse(cards, summaries);
    }
}
