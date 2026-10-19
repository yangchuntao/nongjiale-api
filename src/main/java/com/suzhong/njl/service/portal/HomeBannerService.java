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

/**
 * 首页 Banner 查询服务（门户端）。
 *
 * <p>职责：只返回启用中的 Banner：全站条目始终可见，指定市的条目仅在该市当前仍开通时可见，
 * 按 {@code sort_order, id} 升序。
 *
 * <p>缓存：本接口不进首页 feed 缓存，每次直接查库，保证运营启停后立即生效。
 *
 * <p>只读，无事务；无条目时返回空数组，不返回 null。
 */
@Service
public class HomeBannerService {

    private final HomeBannerMapper homeBannerMapper;
    private final RegionMapper regionMapper;

    public HomeBannerService(HomeBannerMapper homeBannerMapper, RegionMapper regionMapper) {
        this.homeBannerMapper = homeBannerMapper;
        this.regionMapper = regionMapper;
    }

    /**
     * 查询门户可见的 Banner 列表。
     *
     * <p>流程：查启用条目并排序 → 批量加载涉及的市 → 全站条目直接放行，市级条目再判 {@code isOpen}。
     *
     * @return Banner 列表，可能为空
     */
    public List<HomeBannerItem> list() {
        List<HomeBanner> rows = homeBannerMapper.selectList(Wrappers.<HomeBanner>lambdaQuery()
                .eq(HomeBanner::getEnabled, Boolean.TRUE)
                .orderByAsc(HomeBanner::getSortOrder)
                .orderByAsc(HomeBanner::getId));
        Map<Long, Region> cities = citiesOf(rows);
        List<HomeBannerItem> items = new ArrayList<>();
        for (HomeBanner row : rows) {
            // 二次判空是防御：SQL 已按 enabled 过滤，这里避免未来改查询时把停用条目带出去
            if (!Boolean.TRUE.equals(row.getEnabled())) {
                continue;
            }
            if (row.getScope() == BannerScope.ALL) {
                // 全站条目不带城市信息
                items.add(toItem(row, null));
                continue;
            }
            if (row.getScope() != BannerScope.CITY) {
                // 未知投放范围一律不投放，避免脏数据泄漏到首页
                continue;
            }
            Region city = row.getCityId() == null ? null : cities.get(row.getCityId());
            // 投放市已关闭（或地区不存在/未启用）时整条隐藏，运营端启用时已做过一次 409 校验
            if (CityRules.isOpen(city)) {
                items.add(toItem(row, city));
            }
        }
        return items;
    }

    /** 批量加载 Banner 涉及的市，避免逐条查库；没有市级条目时返回空映射。 */
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

    /** 组装对客条目；市级条目才带城市 ID、slug 与名称，全站条目的这三个字段为 null。 */
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
