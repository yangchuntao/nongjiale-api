package com.suzhong.njl.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.RegionOpenChecklist;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.RegionOpenChecklistMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.service.city.CityRules;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.ChecklistPatchRequest;
import com.suzhong.njl.web.admin.dto.CityChecklistView;
import com.suzhong.njl.web.admin.dto.CityDetailResponse;
import com.suzhong.njl.web.admin.dto.CitySummaryResponse;

@Service
public class AdminCityService {

    private final RegionMapper regionMapper;
    private final RegionOpenChecklistMapper checklistMapper;
    private final StoreMapper storeMapper;
    private final ArticleMapper articleMapper;
    private final OpenCityThresholds thresholds;
    private final AuditRecorder auditRecorder;
    private final AdminCache adminCache;

    public AdminCityService(
            RegionMapper regionMapper,
            RegionOpenChecklistMapper checklistMapper,
            StoreMapper storeMapper,
            ArticleMapper articleMapper,
            OpenCityThresholds thresholds,
            AuditRecorder auditRecorder,
            AdminCache adminCache) {
        this.regionMapper = regionMapper;
        this.checklistMapper = checklistMapper;
        this.storeMapper = storeMapper;
        this.articleMapper = articleMapper;
        this.thresholds = thresholds;
        this.auditRecorder = auditRecorder;
        this.adminCache = adminCache;
    }

    public List<CitySummaryResponse> list() {
        List<Region> cities = enabledCities();
        Map<Long, Region> byId = allRegions();
        Map<Long, RegionOpenChecklist> checks = checklists(cities);
        Map<Long, Counts> counts = countByCity(cities);
        List<CitySummaryResponse> items = new ArrayList<>(cities.size());
        for (Region city : cities) {
            RegionOpenChecklist checklist = checks.get(city.getId());
            Counts count = counts.get(city.getId());
            items.add(new CitySummaryResponse(
                    city.getId(),
                    provinceName(city, byId),
                    city.getName(),
                    city.getSlug(),
                    CityRules.effective(city.getOpenStatus()),
                    count.storeCount(),
                    count.articleCount(),
                    checklist == null ? null : checklist.getUpdatedAt()));
        }
        return items;
    }

    public CityDetailResponse detail(Long cityId) {
        Region city = requireEnabledCity(cityId);
        return toDetail(city, checklistMapper.selectById(cityId), countOne(city), allRegions());
    }

    @Transactional
    public CityDetailResponse patch(long adminId, Long cityId, ChecklistPatchRequest request) {
        requireEnabledCity(cityId);
        if (request.supplyReadySpecified() || request.articlesReadySpecified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CityRules.AUTO_FIELD_MESSAGE);
        }
        if (!request.hasManualChange()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请指定要更新的项");
        }
        RegionOpenChecklist locked = lockChecklist(cityId);
        assertVersion(locked, request.getVersion());
        boolean destination = destinationReady(locked);
        boolean support = supportCovered(locked);
        boolean dirty = dirtyDataCleared(locked);
        if (request.destinationReadySpecified() && request.getDestinationReady() != null) {
            destination = request.getDestinationReady();
        }
        if (request.supportCoveredSpecified() && request.getSupportCovered() != null) {
            support = request.getSupportCovered();
        }
        if (request.dirtyDataClearedSpecified() && request.getDirtyDataCleared() != null) {
            dirty = request.getDirtyDataCleared();
        }
        RegionOpenChecklist saved = saveChecklist(cityId, request.getVersion(), locked, destination, support, dirty, adminId);
        Map<String, Object> detail = checklistDetail(destination, support, dirty);
        auditRecorder.record(adminId, AuditAction.CITY_CHECKLIST, AuditTarget.REGION, cityId, detail);
        return toDetail(requireEnabledCity(cityId), saved, countOne(requireEnabledCity(cityId)), allRegions());
    }

    @Transactional
    public CityDetailResponse open(long adminId, Long cityId, long version) {
        RegionOpenChecklist locked = lockChecklist(cityId);
        Region city = requireEnabledCity(cityId);
        if (CityRules.effective(city.getOpenStatus()) == RegionOpenStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "该市已开通");
        }
        assertVersion(locked, version);
        Counts counts = countOne(city);
        boolean destination = destinationReady(locked);
        boolean support = supportCovered(locked);
        boolean dirty = dirtyDataCleared(locked);
        List<String> missing = CityRules.missingReasons(
                destination, support, dirty, counts.storeCount(), counts.articleCount(),
                thresholds.minStores(), thresholds.minArticles());
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, CityRules.openRejectedMessage(missing));
        }
        RegionOpenStatus previous = CityRules.effective(city.getOpenStatus());
        city.setOpenStatus(RegionOpenStatus.OPEN);
        regionMapper.updateById(city);
        RegionOpenChecklist saved = saveChecklist(cityId, version, locked, destination, support, dirty, adminId);
        Map<String, Object> detail = checklistDetail(destination, support, dirty);
        detail.put("from", previous.code());
        detail.put("to", RegionOpenStatus.OPEN.code());
        auditRecorder.record(adminId, AuditAction.CITY_OPEN, AuditTarget.REGION, cityId, detail);
        evictAfterCommit();
        return toDetail(city, saved, counts, allRegions());
    }

    @Transactional
    public CityDetailResponse close(long adminId, Long cityId, long version) {
        RegionOpenChecklist locked = lockChecklist(cityId);
        Region city = requireEnabledCity(cityId);
        if (CityRules.effective(city.getOpenStatus()) != RegionOpenStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "该市未开通");
        }
        assertVersion(locked, version);
        boolean destination = destinationReady(locked);
        boolean support = supportCovered(locked);
        boolean dirty = dirtyDataCleared(locked);
        RegionOpenStatus previous = CityRules.effective(city.getOpenStatus());
        city.setOpenStatus(RegionOpenStatus.CLOSED);
        regionMapper.updateById(city);
        RegionOpenChecklist saved = saveChecklist(cityId, version, locked, destination, support, dirty, adminId);
        Map<String, Object> detail = checklistDetail(destination, support, dirty);
        detail.put("from", previous.code());
        detail.put("to", RegionOpenStatus.CLOSED.code());
        auditRecorder.record(adminId, AuditAction.CITY_CLOSE, AuditTarget.REGION, cityId, detail);
        evictAfterCommit();
        return toDetail(city, saved, countOne(city), allRegions());
    }

    private List<Region> enabledCities() {
        return regionMapper.selectList(Wrappers.<Region>lambdaQuery()
                .eq(Region::getEnabled, true)
                .eq(Region::getLevel, RegionLevel.CITY)
                .orderByAsc(Region::getSortOrder)
                .orderByAsc(Region::getId));
    }

    private Region requireEnabledCity(Long cityId) {
        Region region = cityId == null ? null : regionMapper.selectById(cityId);
        if (region == null || region.getLevel() != RegionLevel.CITY) {
            throw new ApiException(HttpStatus.NOT_FOUND, "城市不存在");
        }
        if (!Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
        return region;
    }

    private RegionOpenChecklist lockChecklist(Long cityId) {
        return checklistMapper.selectOne(Wrappers.<RegionOpenChecklist>lambdaQuery()
                .eq(RegionOpenChecklist::getCityId, cityId)
                .last("FOR UPDATE"));
    }

    private void assertVersion(RegionOpenChecklist locked, Long version) {
        if (version == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "版本不能为空");
        }
        long current = locked == null ? 0L : locked.getVersion();
        if (locked == null) {
            if (version != 0L) {
                throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
            }
            return;
        }
        if (current != version) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
    }

    private RegionOpenChecklist saveChecklist(
            Long cityId,
            Long version,
            RegionOpenChecklist locked,
            boolean destinationReady,
            boolean supportCovered,
            boolean dirtyDataCleared,
            long adminId) {
        if (locked == null) {
            RegionOpenChecklist created = new RegionOpenChecklist();
            created.setCityId(cityId);
            created.setDestinationReady(destinationReady);
            created.setSupportCovered(supportCovered);
            created.setDirtyDataCleared(dirtyDataCleared);
            created.setVersion(1L);
            created.setUpdatedBy(adminId);
            try {
                checklistMapper.insert(created);
            } catch (DuplicateKeyException ex) {
                throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
            }
            return checklistMapper.selectById(cityId);
        }
        locked.setDestinationReady(destinationReady);
        locked.setSupportCovered(supportCovered);
        locked.setDirtyDataCleared(dirtyDataCleared);
        locked.setUpdatedBy(adminId);
        locked.setVersion(version);
        int updated = checklistMapper.updateById(locked);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "数据已被修改");
        }
        return checklistMapper.selectById(cityId);
    }

    private boolean destinationReady(RegionOpenChecklist checklist) {
        return checklist != null && Boolean.TRUE.equals(checklist.getDestinationReady());
    }

    private boolean supportCovered(RegionOpenChecklist checklist) {
        return checklist != null && Boolean.TRUE.equals(checklist.getSupportCovered());
    }

    private boolean dirtyDataCleared(RegionOpenChecklist checklist) {
        return checklist != null && Boolean.TRUE.equals(checklist.getDirtyDataCleared());
    }

    private Map<String, Object> checklistDetail(boolean destination, boolean support, boolean dirty) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("destinationReady", destination);
        detail.put("supportCovered", support);
        detail.put("dirtyDataCleared", dirty);
        return detail;
    }

    private void evictAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            adminCache.evictCityOpen();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                adminCache.evictCityOpen();
            }
        });
    }

    private CityDetailResponse toDetail(
            Region city,
            RegionOpenChecklist checklist,
            Counts counts,
            Map<Long, Region> byId) {
        boolean destination = destinationReady(checklist);
        boolean support = supportCovered(checklist);
        boolean dirty = dirtyDataCleared(checklist);
        int minStores = thresholds.minStores();
        int minArticles = thresholds.minArticles();
        boolean supplyReady = counts.storeCount() >= minStores;
        boolean articlesReady = counts.articleCount() >= minArticles;
        List<String> missing = CityRules.missingReasons(
                destination, support, dirty, counts.storeCount(), counts.articleCount(), minStores, minArticles);
        boolean open = CityRules.effective(city.getOpenStatus()) == RegionOpenStatus.OPEN;
        return new CityDetailResponse(
                city.getId(),
                provinceName(city, byId),
                city.getName(),
                city.getSlug(),
                CityRules.effective(city.getOpenStatus()),
                counts.storeCount(),
                counts.articleCount(),
                checklist == null ? null : checklist.getUpdatedAt(),
                Boolean.TRUE,
                minStores,
                minArticles,
                new CityChecklistView(destination, supplyReady, articlesReady, support, dirty),
                !open && missing.isEmpty(),
                open,
                missing,
                checklist == null || checklist.getVersion() == null ? 0L : checklist.getVersion(),
                checklist == null ? null : checklist.getUpdatedBy());
    }

    private Counts countOne(Region city) {
        return countByCity(List.of(city)).get(city.getId());
    }

    private Map<Long, Counts> countByCity(List<Region> cities) {
        Map<Long, Counts> result = new HashMap<>();
        if (cities.isEmpty()) {
            return result;
        }
        Set<Long> cityIds = new HashSet<>();
        for (Region city : cities) {
            cityIds.add(city.getId());
            result.put(city.getId(), new Counts(0, 0));
        }
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery().eq(Region::getEnabled, true));
        Map<Long, Region> byId = new HashMap<>();
        List<Long> supplyRegionIds = new ArrayList<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
            for (Long cityId : cityIds) {
                if (CityRules.countsTowardCity(region, cityId)) {
                    supplyRegionIds.add(region.getId());
                    break;
                }
            }
        }
        if (supplyRegionIds.isEmpty()) {
            return result;
        }
        List<Store> stores = storeMapper.selectList(Wrappers.<Store>lambdaQuery()
                .eq(Store::getShelfStatus, ShelfStatus.ON_SHELF)
                .eq(Store::getTestData, Boolean.FALSE)
                .in(Store::getRegionId, supplyRegionIds));
        for (Store store : stores) {
            addStore(result, byId.get(store.getRegionId()), cityIds);
        }
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .in(Article::getRegionId, supplyRegionIds));
        for (Article article : articles) {
            addArticle(result, byId.get(article.getRegionId()), cityIds);
        }
        return result;
    }

    private void addStore(Map<Long, Counts> result, Region region, Set<Long> cityIds) {
        for (Long cityId : cityIds) {
            if (CityRules.countsTowardCity(region, cityId)) {
                Counts current = result.get(cityId);
                result.put(cityId, new Counts(current.storeCount() + 1, current.articleCount()));
                return;
            }
        }
    }

    private void addArticle(Map<Long, Counts> result, Region region, Set<Long> cityIds) {
        for (Long cityId : cityIds) {
            if (CityRules.countsTowardCity(region, cityId)) {
                Counts current = result.get(cityId);
                result.put(cityId, new Counts(current.storeCount(), current.articleCount() + 1));
                return;
            }
        }
    }

    private Map<Long, RegionOpenChecklist> checklists(List<Region> cities) {
        Map<Long, RegionOpenChecklist> byCity = new HashMap<>();
        if (cities.isEmpty()) {
            return byCity;
        }
        List<Long> ids = new ArrayList<>(cities.size());
        for (Region city : cities) {
            ids.add(city.getId());
        }
        List<RegionOpenChecklist> rows = checklistMapper.selectList(
                Wrappers.<RegionOpenChecklist>lambdaQuery().in(RegionOpenChecklist::getCityId, ids));
        for (RegionOpenChecklist row : rows) {
            byCity.put(row.getCityId(), row);
        }
        return byCity;
    }

    private Map<Long, Region> allRegions() {
        Map<Long, Region> byId = new HashMap<>();
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        for (Region region : regions) {
            byId.put(region.getId(), region);
        }
        return byId;
    }

    private String provinceName(Region city, Map<Long, Region> byId) {
        if (city.getParentId() == null) {
            return null;
        }
        Region parent = byId.get(city.getParentId());
        if (parent == null || parent.getLevel() != RegionLevel.PROVINCE) {
            return null;
        }
        return parent.getName();
    }

    private record Counts(long storeCount, long articleCount) {
    }
}
