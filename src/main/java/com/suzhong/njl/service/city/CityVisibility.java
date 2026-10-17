package com.suzhong.njl.service.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.DestinationStatus;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.ApiException;

@Service
public class CityVisibility {

    private final RegionMapper regionMapper;

    public CityVisibility(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    public Map<Long, Region> byId() {
        return index(regionMapper.selectList(Wrappers.<Region>lambdaQuery()));
    }

    public boolean bookableRegion(Long regionId) {
        return isCustomerStoreRegion(regionId, byId());
    }

    public boolean isCustomerStoreRegion(Long regionId, Map<Long, Region> byId) {
        if (regionId == null || byId == null) {
            return false;
        }
        Region region = byId.get(regionId);
        if (region == null || !Boolean.TRUE.equals(region.getEnabled())) {
            return false;
        }
        if (region.getLevel() == RegionLevel.CITY) {
            return CityRules.isOpen(region);
        }
        if (region.getLevel() == RegionLevel.DISTRICT) {
            Region parent = region.getParentId() == null ? null : byId.get(region.getParentId());
            return CityRules.isOpen(parent);
        }
        return false;
    }

    public StoreListingScope listScope(Long regionId) {
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        Map<Long, Region> byId = index(regions);
        if (regionId == null) {
            return new StoreListingScope(openMarketIds(regions, null), DestinationStatus.NONE, null);
        }
        Region region = byId.get(regionId);
        if (region == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不存在");
        }
        if (!Boolean.TRUE.equals(region.getEnabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "地区不可用");
        }
        if (region.getLevel() == RegionLevel.CITY) {
            if (!CityRules.isOpen(region)) {
                return StoreListingScope.closed();
            }
            return new StoreListingScope(openMarketIds(regions, region.getId()), DestinationStatus.OPEN, null);
        }
        if (region.getLevel() == RegionLevel.DISTRICT) {
            Region parent = region.getParentId() == null ? null : byId.get(region.getParentId());
            if (!CityRules.isOpen(parent)) {
                return StoreListingScope.closed();
            }
            return new StoreListingScope(List.of(region.getId()), DestinationStatus.OPEN, null);
        }
        return new StoreListingScope(openMarketIds(regions, region.getId()), DestinationStatus.NONE, null);
    }

    /**
     * 门户攻略列表和首页可以露出的地区：空地区另计；省；已开通的市及其启用区县。
     */
    public Set<Long> promotedArticleRegionIds() {
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        Set<Long> openCityIds = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        for (Region region : regions) {
            if (region.getLevel() == RegionLevel.PROVINCE) {
                ids.add(region.getId());
            }
            if (CityRules.isOpen(region)) {
                openCityIds.add(region.getId());
                ids.add(region.getId());
            }
        }
        for (Region region : regions) {
            if (region.getLevel() == RegionLevel.DISTRICT
                    && Boolean.TRUE.equals(region.getEnabled())
                    && region.getParentId() != null
                    && openCityIds.contains(region.getParentId())) {
                ids.add(region.getId());
            }
        }
        return ids;
    }

    private List<Long> openMarketIds(List<Region> regions, Long anchorId) {
        Set<Long> cityIds = new HashSet<>();
        for (Region region : regions) {
            if (!CityRules.isOpen(region)) {
                continue;
            }
            if (anchorId == null || anchorId.equals(region.getId()) || anchorId.equals(region.getParentId())) {
                cityIds.add(region.getId());
            }
        }
        List<Long> ids = new ArrayList<>(cityIds);
        for (Region region : regions) {
            if (region.getLevel() == RegionLevel.DISTRICT
                    && Boolean.TRUE.equals(region.getEnabled())
                    && region.getParentId() != null
                    && cityIds.contains(region.getParentId())) {
                ids.add(region.getId());
            }
        }
        return ids;
    }

    private Map<Long, Region> index(List<Region> regions) {
        Map<Long, Region> byId = new HashMap<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
        }
        return byId;
    }
}
