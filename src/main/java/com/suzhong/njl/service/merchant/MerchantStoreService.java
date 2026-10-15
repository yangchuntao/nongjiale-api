package com.suzhong.njl.service.merchant;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.merchant.dto.MerchantStoreResponse;
import com.suzhong.njl.web.merchant.dto.MerchantStoreUpdateRequest;

@Service
public class MerchantStoreService {

    private static final TypeReference<List<String>> GALLERY_TYPE = new TypeReference<>() {
    };

    private final MerchantAccess merchantAccess;
    private final StoreMapper storeMapper;
    private final RegionMapper regionMapper;
    private final CacheAside cacheAside;
    private final ObjectMapper objectMapper;

    public MerchantStoreService(
            MerchantAccess merchantAccess,
            StoreMapper storeMapper,
            RegionMapper regionMapper,
            CacheAside cacheAside,
            ObjectMapper objectMapper) {
        this.merchantAccess = merchantAccess;
        this.storeMapper = storeMapper;
        this.regionMapper = regionMapper;
        this.cacheAside = cacheAside;
        this.objectMapper = objectMapper;
    }

    public PageResult<MerchantStoreResponse> list(AuthenticatedUser principal, Integer page, Integer pageSize) {
        long orgId = merchantAccess.requireOrgId(principal);
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = storeMapper.selectCount(Wrappers.<Store>lambdaQuery().eq(Store::getMerchantOrgId, orgId));
        List<Store> rows = storeMapper.selectList(Wrappers.<Store>lambdaQuery()
                .eq(Store::getMerchantOrgId, orgId)
                .orderByAsc(Store::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<MerchantStoreResponse> items = new ArrayList<>(rows.size());
        for (Store row : rows) {
            items.add(toResponse(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public MerchantStoreResponse detail(AuthenticatedUser principal, Long id) {
        long orgId = merchantAccess.requireOrgId(principal);
        return toResponse(requireOwned(orgId, id));
    }

    @Transactional
    public MerchantStoreResponse update(AuthenticatedUser principal, Long id, MerchantStoreUpdateRequest request) {
        long orgId = merchantAccess.requireOrgId(principal);
        Store store = requireOwned(orgId, id);
        Region region = regionMapper.selectById(request.regionId());
        if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
        ShelfStatusKeep shelf = ShelfStatusKeep.of(store);
        store.setName(request.name());
        store.setRegionId(request.regionId());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setCoverUrl(request.coverUrl());
        if (request.gallery() != null) {
            store.setGalleryJson(galleryJson(request.gallery()));
        }
        store.setPriceText(request.priceText());
        store.setPriceFrom(request.priceFrom());
        store.setPackageText(request.packageText());
        store.setFacilities(request.facilities());
        store.setFeatures(request.features());
        store.setNotice(request.notice());
        store.setTrafficText(request.trafficText());
        store.setVersion(request.version());
        shelf.restore(store);
        int updated = storeMapper.updateById(store);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        cacheAside.evict(CacheName.STORE_DETAIL, String.valueOf(store.getId()));
        cacheAside.evict(CacheName.HOME_FEED, "default");
        return toResponse(requireOwned(orgId, id));
    }

    Store requireOwned(long orgId, Long id) {
        Store store = storeMapper.selectOne(Wrappers.<Store>lambdaQuery()
                .eq(Store::getId, id)
                .eq(Store::getMerchantOrgId, orgId));
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "门店不存在");
        }
        return store;
    }

    private MerchantStoreResponse toResponse(Store store) {
        return new MerchantStoreResponse(
                store.getId(),
                store.getMerchantOrgId(),
                store.getRegionId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                store.getCoverUrl(),
                gallery(store.getGalleryJson()),
                store.getPriceText(),
                store.getPriceFrom(),
                store.getPackageText(),
                store.getFacilities(),
                store.getFeatures(),
                store.getNotice(),
                store.getTrafficText(),
                store.getShelfStatus(),
                store.getHomeRecommended(),
                store.getHomeSort(),
                store.getTestData(),
                store.getVersion());
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

    private String galleryJson(List<String> gallery) {
        if (gallery == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(gallery);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图集格式不正确");
        }
    }

    /**
     * 商家更新只改文案和地址。上下架、首页推荐在写回前按原值钉住。
     */
    private record ShelfStatusKeep(com.suzhong.njl.domain.enums.ShelfStatus shelfStatus, Boolean homeRecommended, Integer homeSort, Boolean testData, Long merchantOrgId) {

        static ShelfStatusKeep of(Store store) {
            return new ShelfStatusKeep(
                    store.getShelfStatus(),
                    store.getHomeRecommended(),
                    store.getHomeSort(),
                    store.getTestData(),
                    store.getMerchantOrgId());
        }

        void restore(Store store) {
            store.setShelfStatus(shelfStatus);
            store.setHomeRecommended(homeRecommended);
            store.setHomeSort(homeSort);
            store.setTestData(testData);
            store.setMerchantOrgId(merchantOrgId);
        }
    }
}
