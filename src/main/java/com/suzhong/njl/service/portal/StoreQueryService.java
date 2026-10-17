package com.suzhong.njl.service.portal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.entity.StorePackage;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.domain.enums.StoreSort;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.mapper.StorePackageMapper;
import com.suzhong.njl.mapper.StoreSearch;
import com.suzhong.njl.service.city.CityVisibility;
import com.suzhong.njl.service.city.StoreListingScope;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.service.support.TestDataMark;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.portal.dto.RegionItem;
import com.suzhong.njl.web.portal.dto.StoreCard;
import com.suzhong.njl.web.portal.dto.StoreDetailResponse;
import com.suzhong.njl.web.portal.dto.StoreListResponse;
import com.suzhong.njl.web.portal.dto.StorePackageItem;

@Service
public class StoreQueryService {

    private static final TypeReference<StoreDetailResponse> DETAIL_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> GALLERY_TYPE = new TypeReference<>() {
    };

    private final StoreMapper storeMapper;
    private final StorePackageMapper storePackageMapper;
    private final RegionQueryService regionQueryService;
    private final CityVisibility cityVisibility;
    private final CacheAside cacheAside;
    private final ObjectMapper objectMapper;

    public StoreQueryService(
            StoreMapper storeMapper,
            StorePackageMapper storePackageMapper,
            RegionQueryService regionQueryService,
            CityVisibility cityVisibility,
            CacheAside cacheAside,
            ObjectMapper objectMapper) {
        this.storeMapper = storeMapper;
        this.storePackageMapper = storePackageMapper;
        this.regionQueryService = regionQueryService;
        this.cityVisibility = cityVisibility;
        this.cacheAside = cacheAside;
        this.objectMapper = objectMapper;
    }

    public StoreListResponse list(
            Long regionId,
            String tag,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sort,
            Integer page,
            Integer pageSize) {
        StoreSort storeSort = parseSort(sort);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        StoreListingScope scope = cityVisibility.listScope(regionId);
        if (scope.regionIds().isEmpty()) {
            return new StoreListResponse(
                    List.of(), 0, pageQuery.page(), pageQuery.pageSize(), scope.destinationStatus(), scope.emptyHint());
        }
        StoreSearch query = new StoreSearch();
        query.setShelfStatus(ShelfStatus.ON_SHELF);
        query.setRegionIds(scope.regionIds());
        query.setTagCode(blankToNull(tag));
        query.setPriceMin(priceMin);
        query.setPriceMax(priceMax);
        query.applySort(storeSort);
        query.setLimit(pageQuery.pageSize());
        query.setOffset(pageQuery.offset());
        long total = storeMapper.countOnShelf(query);
        List<Store> rows = total == 0 ? List.of() : storeMapper.searchOnShelf(query);
        Map<Long, String> regionNames = regionNames();
        List<StoreCard> items = new ArrayList<>(rows.size());
        for (Store row : rows) {
            items.add(toCard(row, regionNames));
        }
        return new StoreListResponse(
                items, total, pageQuery.page(), pageQuery.pageSize(), scope.destinationStatus(), scope.emptyHint());
    }

    public StoreDetailResponse detail(Long id) {
        Store store = requireBookable(id);
        return cacheAside.get(CacheName.STORE_DETAIL, String.valueOf(id), DETAIL_TYPE, () -> loadDetail(store));
    }

    public Store requireBookable(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "房源不存在");
        }
        if (store.getShelfStatus() != ShelfStatus.ON_SHELF || !cityVisibility.bookableRegion(store.getRegionId())) {
            throw new ApiException(HttpStatus.GONE, "已下架或不可用");
        }
        return store;
    }

    private StoreDetailResponse loadDetail(Store store) {
        List<StorePackage> packages = storePackageMapper.selectList(
                Wrappers.<StorePackage>lambdaQuery()
                        .eq(StorePackage::getStoreId, store.getId())
                        .orderByAsc(StorePackage::getSortOrder)
                        .orderByAsc(StorePackage::getId));
        List<StorePackageItem> items = new ArrayList<>(packages.size());
        for (StorePackage row : packages) {
            items.add(new StorePackageItem(
                    row.getId(),
                    row.getName(),
                    row.getPriceFrom(),
                    row.getDescription(),
                    row.getSortOrder()));
        }
        return new StoreDetailResponse(
                store.getId(),
                store.getName(),
                store.getRegionId(),
                regionNames().get(store.getRegionId()),
                store.getAddress(),
                store.getPhone(),
                store.getCoverUrl(),
                gallery(store.getGalleryJson()),
                store.getPriceText(),
                store.getPriceFrom(),
                store.getPackageText(),
                items,
                store.getFacilities(),
                store.getFeatures(),
                store.getNotice(),
                store.getTrafficText(),
                store.getShelfStatus(),
                store.getHomeRecommended(),
                store.getHomeSort(),
                store.getTestData(),
                TestDataMark.label(store.getTestData()),
                store.getVersion());
    }

    StoreCard toCard(Store store, Map<Long, String> regionNames) {
        return new StoreCard(
                store.getId(),
                store.getName(),
                store.getRegionId(),
                regionNames.get(store.getRegionId()),
                store.getCoverUrl(),
                store.getPriceText(),
                store.getPriceFrom(),
                store.getHomeRecommended(),
                store.getHomeSort(),
                store.getTestData(),
                TestDataMark.label(store.getTestData()),
                store.getShelfStatus());
    }

    private Map<Long, String> regionNames() {
        Map<Long, String> names = new HashMap<>();
        for (RegionItem region : regionQueryService.enabledRegions()) {
            names.put(region.id(), region.name());
        }
        return names;
    }

    private List<String> gallery(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, GALLERY_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private StoreSort parseSort(String sort) {
        try {
            return StoreSort.fromParam(sort);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
