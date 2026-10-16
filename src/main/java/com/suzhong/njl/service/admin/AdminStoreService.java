package com.suzhong.njl.service.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.support.PageQuery;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.AdminStoreCreateRequest;
import com.suzhong.njl.web.admin.dto.AdminStoreResponse;
import com.suzhong.njl.web.admin.dto.AdminStoreUpdateRequest;

@Service
public class AdminStoreService {

    private static final TypeReference<List<String>> GALLERY_TYPE = new TypeReference<>() {
    };

    private final StoreMapper storeMapper;
    private final RegionMapper regionMapper;
    private final MerchantOrgMapper merchantOrgMapper;
    private final AuditRecorder auditRecorder;
    private final AdminCache adminCache;
    private final ObjectMapper objectMapper;

    public AdminStoreService(
            StoreMapper storeMapper,
            RegionMapper regionMapper,
            MerchantOrgMapper merchantOrgMapper,
            AuditRecorder auditRecorder,
            AdminCache adminCache,
            ObjectMapper objectMapper) {
        this.storeMapper = storeMapper;
        this.regionMapper = regionMapper;
        this.merchantOrgMapper = merchantOrgMapper;
        this.auditRecorder = auditRecorder;
        this.adminCache = adminCache;
        this.objectMapper = objectMapper;
    }

    public PageResult<AdminStoreResponse> list(
            ShelfStatus shelfStatus,
            Long regionId,
            Long merchantOrgId,
            Boolean homeRecommended,
            String name,
            Integer page,
            Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(page, pageSize);
        Long total = storeMapper.selectCount(filter(shelfStatus, regionId, merchantOrgId, homeRecommended, name));
        List<Store> rows = storeMapper.selectList(filter(shelfStatus, regionId, merchantOrgId, homeRecommended, name)
                .orderByDesc(Store::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<AdminStoreResponse> items = new ArrayList<>(rows.size());
        for (Store row : rows) {
            items.add(toResponse(row));
        }
        return new PageResult<>(items, total == null ? 0 : total, pageQuery.page(), pageQuery.pageSize());
    }

    public AdminStoreResponse detail(Long id) {
        return toResponse(requireStore(id));
    }

    @Transactional
    public AdminStoreResponse create(Long orgId, AdminStoreCreateRequest request) {
        MerchantOrg org = merchantOrgMapper.selectById(orgId);
        if (org == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "商家不存在");
        }
        requireRegion(request.regionId());
        Store store = new Store();
        store.setMerchantOrgId(orgId);
        store.setRegionId(request.regionId());
        store.setName(request.name());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setCoverUrl(request.coverUrl());
        store.setGalleryJson(request.gallery() == null ? "[]" : galleryJson(request.gallery()));
        store.setPriceText(request.priceText());
        store.setPriceFrom(request.priceFrom());
        store.setPackageText(request.packageText());
        store.setFacilities(request.facilities());
        store.setFeatures(request.features());
        store.setNotice(request.notice());
        store.setTrafficText(request.trafficText());
        store.setShelfStatus(request.shelfStatus() == null ? ShelfStatus.DRAFT : request.shelfStatus());
        store.setHomeRecommended(Boolean.TRUE.equals(request.homeRecommended()));
        store.setHomeSort(request.homeSort() == null ? 100 : request.homeSort());
        store.setTestData(Boolean.TRUE.equals(request.testData()));
        store.setVersion(0L);
        storeMapper.insert(store);
        adminCache.evictStore(store.getId());
        return toResponse(requireStore(store.getId()));
    }

    /**
     * 代编文案、地址、首页推荐，并允许直接改 shelfStatus。
     * 上下架与当前值不同时立即生效，写 SHELF_FORCE，不改申请队列。
     */
    @Transactional
    public AdminStoreResponse update(long adminId, Long id, AdminStoreUpdateRequest request) {
        Store store = requireStore(id);
        requireRegion(request.regionId());
        ShelfStatus previousShelf = store.getShelfStatus();
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
        store.setHomeRecommended(request.homeRecommended());
        store.setHomeSort(request.homeSort());
        store.setShelfStatus(request.shelfStatus());
        if (request.testData() != null) {
            store.setTestData(request.testData());
        }
        store.setVersion(request.version());
        int updated = storeMapper.updateById(store);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        if (previousShelf != request.shelfStatus()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("from", previousShelf == null ? "" : previousShelf.code());
            detail.put("to", request.shelfStatus().code());
            auditRecorder.record(adminId, AuditAction.SHELF_FORCE, AuditTarget.STORE, id, detail);
        }
        adminCache.evictStore(id);
        return toResponse(requireStore(id));
    }

    private LambdaQueryWrapper<Store> filter(
            ShelfStatus shelfStatus,
            Long regionId,
            Long merchantOrgId,
            Boolean homeRecommended,
            String name) {
        LambdaQueryWrapper<Store> query = Wrappers.lambdaQuery();
        if (shelfStatus != null) {
            query.eq(Store::getShelfStatus, shelfStatus);
        }
        if (regionId != null) {
            query.eq(Store::getRegionId, regionId);
        }
        if (merchantOrgId != null) {
            query.eq(Store::getMerchantOrgId, merchantOrgId);
        }
        if (homeRecommended != null) {
            query.eq(Store::getHomeRecommended, homeRecommended);
        }
        if (name != null && !name.isBlank()) {
            query.like(Store::getName, name.trim());
        }
        return query;
    }

    private Store requireStore(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "门店不存在");
        }
        return store;
    }

    private void requireRegion(Long regionId) {
        Region region = regionMapper.selectById(regionId);
        if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
    }

    private AdminStoreResponse toResponse(Store store) {
        return new AdminStoreResponse(
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
        try {
            return objectMapper.writeValueAsString(gallery);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图集格式不正确");
        }
    }
}
