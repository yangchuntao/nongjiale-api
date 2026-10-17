package com.suzhong.njl.service.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.service.city.CityRules;
import com.suzhong.njl.web.portal.dto.RegionItem;

@Service
public class RegionQueryService {

    private static final TypeReference<List<RegionItem>> TYPE = new TypeReference<>() {
    };

    private final RegionMapper regionMapper;
    private final CacheAside cacheAside;

    public RegionQueryService(RegionMapper regionMapper, CacheAside cacheAside) {
        this.regionMapper = regionMapper;
        this.cacheAside = cacheAside;
    }

    public List<RegionItem> enabledRegions() {
        return cacheAside.get(CacheName.REGION, "enabled", TYPE, this::load);
    }

    private List<RegionItem> load() {
        List<Region> rows = regionMapper.selectList(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getEnabled, true)
                        .orderByAsc(Region::getSortOrder)
                        .orderByAsc(Region::getId));
        Map<Long, Region> byId = new HashMap<>();
        Set<Long> provincesWithOpenCity = new HashSet<>();
        for (Region row : rows) {
            byId.put(row.getId(), row);
            if (CityRules.isOpen(row) && row.getParentId() != null) {
                provincesWithOpenCity.add(row.getParentId());
            }
        }
        List<RegionItem> items = new ArrayList<>();
        for (Region row : rows) {
            if (!visible(row, byId, provincesWithOpenCity)) {
                continue;
            }
            items.add(new RegionItem(
                    row.getId(),
                    row.getParentId(),
                    row.getSlug(),
                    row.getName(),
                    row.getLevel(),
                    row.getSortOrder(),
                    row.getSeedMarket()));
        }
        return items;
    }

    private boolean visible(Region row, Map<Long, Region> byId, Set<Long> provincesWithOpenCity) {
        if (row.getLevel() == RegionLevel.PROVINCE) {
            return provincesWithOpenCity.contains(row.getId());
        }
        if (row.getLevel() == RegionLevel.CITY) {
            return CityRules.isOpen(row);
        }
        if (row.getLevel() == RegionLevel.DISTRICT) {
            return CityRules.isOpen(byId.get(row.getParentId()));
        }
        return false;
    }
}
