package com.suzhong.njl.service.portal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.suzhong.njl.cache.CacheAside;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.mapper.RegionMapper;
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

    public List<Long> selfAndDescendants(Long regionId) {
        if (regionId == null) {
            return List.of();
        }
        List<RegionItem> regions = enabledRegions();
        Set<Long> ids = new HashSet<>();
        ids.add(regionId);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (RegionItem region : regions) {
                if (region.parentId() != null && ids.contains(region.parentId()) && ids.add(region.id())) {
                    grew = true;
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private List<RegionItem> load() {
        List<Region> rows = regionMapper.selectList(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getEnabled, true)
                        .orderByAsc(Region::getSortOrder)
                        .orderByAsc(Region::getId));
        List<RegionItem> items = new ArrayList<>(rows.size());
        for (Region row : rows) {
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
}
