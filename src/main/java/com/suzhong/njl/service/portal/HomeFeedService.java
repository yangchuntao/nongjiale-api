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

@Service
public class HomeFeedService {

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
        Map<Long, Region> regions = cityVisibility.byId();
        List<StoreCard> cards = new ArrayList<>();
        for (Store store : stores) {
            if (!cityVisibility.isCustomerStoreRegion(store.getRegionId(), regions)) {
                continue;
            }
            cards.add(storeQueryService.toCard(store, regionNames));
        }
        List<ArticleSummary> summaries = articleQueryService.latestPromoted(ARTICLE_LIMIT);
        return new HomeFeedResponse(cards, summaries);
    }
}
