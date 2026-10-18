package com.suzhong.njl.service.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.HomeBanner;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.mapper.HomeBannerMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.service.city.CityRules;
import com.suzhong.njl.web.portal.dto.HomeBannerItem;

@Service
public class HomeBannerService {

    private final HomeBannerMapper homeBannerMapper;
    private final RegionMapper regionMapper;

    public HomeBannerService(HomeBannerMapper homeBannerMapper, RegionMapper regionMapper) {
        this.homeBannerMapper = homeBannerMapper;
        this.regionMapper = regionMapper;
    }

    public List<HomeBannerItem> list() {
        List<HomeBanner> rows = homeBannerMapper.selectList(Wrappers.<HomeBanner>lambdaQuery()
                .eq(HomeBanner::getEnabled, Boolean.TRUE)
                .orderByAsc(HomeBanner::getSortOrder)
                .orderByAsc(HomeBanner::getId));
        Map<Long, Region> cities = citiesOf(rows);
        List<HomeBannerItem> items = new ArrayList<>();
        for (HomeBanner row : rows) {
            if (!Boolean.TRUE.equals(row.getEnabled())) {
                continue;
            }
            if (row.getScope() == BannerScope.ALL) {
                items.add(toItem(row, null));
                continue;
            }
            if (row.getScope() != BannerScope.CITY) {
                continue;
            }
            Region city = row.getCityId() == null ? null : cities.get(row.getCityId());
            if (CityRules.isOpen(city)) {
                items.add(toItem(row, city));
            }
        }
        return items;
    }

    private Map<Long, Region> citiesOf(List<HomeBanner> rows) {
        Set<Long> ids = new LinkedHashSet<>();
        for (HomeBanner row : rows) {
            if (row.getScope() == BannerScope.CITY && row.getCityId() != null) {
                ids.add(row.getCityId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Region> regions = regionMapper.selectList(Wrappers.<Region>lambdaQuery().in(Region::getId, ids));
        Map<Long, Region> byId = new HashMap<>();
        for (Region region : regions) {
            byId.put(region.getId(), region);
        }
        return byId;
    }

    private HomeBannerItem toItem(HomeBanner banner, Region city) {
        boolean cityScope = banner.getScope() == BannerScope.CITY && city != null;
        return new HomeBannerItem(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getScope(),
                cityScope ? city.getId() : null,
                cityScope ? city.getSlug() : null,
                cityScope ? city.getName() : null,
                banner.getSortOrder(),
                Boolean.TRUE);
    }
}
