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

/**
 * 门户地区查询服务。
 *
 * <p>职责：给 {@code GET /api/regions} 提供目的地树原料，只暴露「已开通市 + 其启用区县 +
 * 至少有一个开通市的省」，未开通市及其区县不出现。
 *
 * <p>缓存：键 {@code region:enabled}，TTL 见 {@code app.cache.region-ttl}（默认 30 分钟）。
 * 键名沿用 {@code enabled} 是既有契约，开城/关城必须删除这把完整键，否则门户最长 30 分钟仍是旧目的地。
 *
 * <p>只读，无事务；Redis 不可用时由 {@link CacheAside} 降级为直接查库。
 */
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

    /**
     * 查询门户可见地区列表。
     *
     * <p>结果按 {@code sort_order, id} 升序，已含开通过滤；调用方不需要再判开通态。
     *
     * @return 地区项列表，可能为空；命中缓存时不查库
     */
    public List<RegionItem> enabledRegions() {
        return cacheAside.get(CacheName.REGION, "enabled", TYPE, this::load);
    }

    /** 从库加载并过滤地区，组装成门户地区项；过滤逻辑见 {@link #visible}。 */
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
            // 先收齐「有开通市」的省，省是否返回只取决于这一点，与省自身排序无关
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

    /** 三级可见性：省看是否至少有一个开通市；市看自身 OPEN；区县看父市是否 OPEN。 */
    private boolean visible(Region row, Map<Long, Region> byId, Set<Long> provincesWithOpenCity) {
        if (row.getLevel() == RegionLevel.PROVINCE) {
            // 没有开通市的省整省不返回，避免用户点进去看到空目的地
            return provincesWithOpenCity.contains(row.getId());
        }
        if (row.getLevel() == RegionLevel.CITY) {
            return CityRules.isOpen(row);
        }
        if (row.getLevel() == RegionLevel.DISTRICT) {
            // 区县不单独开城；父节点缺失或父市 CLOSED 都不返回
            return CityRules.isOpen(byId.get(row.getParentId()));
        }
        return false;
    }
}
